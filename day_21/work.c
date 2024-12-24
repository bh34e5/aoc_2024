#include <assert.h>
#include <ctype.h>
#include <limits.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>

#define INLINE_BUF (1 << 3)

#ifndef MAX_RECUR
#define MAX_RECUR 25
#endif

struct pos {
    char r;
    char c;
};

struct offset {
    char dr;
    char dc;
};

enum numpad_key {
    NP_0,
    NP_1,
    NP_2,
    NP_3,
    NP_4,
    NP_5,
    NP_6,
    NP_7,
    NP_8,
    NP_9,
    NP_A,
};

enum arrpad_key {
    AP_U,
    AP_D,
    AP_L,
    AP_R,
    AP_A,
};
#define ARR_PAD_ROWS 2
#define ARR_PAD_COLS 3

static struct pos const numpad_posns[] = {
    [NP_1] = {.r = 2, .c = 0}, [NP_2] = {.r = 2, .c = 1},
    [NP_3] = {.r = 2, .c = 2}, [NP_4] = {.r = 1, .c = 0},
    [NP_5] = {.r = 1, .c = 1}, [NP_6] = {.r = 1, .c = 2},
    [NP_7] = {.r = 0, .c = 0}, [NP_8] = {.r = 0, .c = 1},
    [NP_9] = {.r = 0, .c = 2}, [NP_0] = {.r = 3, .c = 1},
    [NP_A] = {.r = 3, .c = 2},
};

static struct pos const arrpad_posns[] = {
    [AP_U] = {.r = 0, .c = 1}, [AP_D] = {.r = 1, .c = 1},
    [AP_L] = {.r = 1, .c = 0}, [AP_R] = {.r = 1, .c = 2},
    [AP_A] = {.r = 0, .c = 2},
};

static struct pos const def_num_pos = numpad_posns[NP_A];
static struct pos const def_arr_pos = arrpad_posns[AP_A];
static struct pos const invalid_num = {.r = 3, .c = 0};
static struct pos const invalid_arr = {.r = 0, .c = 0};

struct code {
    char *keys;
    ulong len;
    ulong cap;
};

struct code_view {
    char const *keys;
    ulong len;
};

#define VIEW_OF(s, l, o)                                                       \
    ({                                                                         \
        __auto_type _s = (s);                                                  \
        ulong _l = (l);                                                        \
        ulong _o = (o);                                                        \
        assert(_s.len >= (_l + _o));                                           \
        ((struct code_view){.keys = _s.keys + _o, .len = _l});                 \
    })
#define VIEW_OF_TAIL(s, o)                                                     \
    ({                                                                         \
        __auto_type _st = (s);                                                 \
        struct code_view _vt = {.keys = _st.keys, .len = _st.len};             \
        ulong _ot = (o);                                                       \
        assert(_ot <= _vt.len);                                                \
        VIEW_OF(_vt, _vt.len - _ot, _ot);                                      \
    })
#define VIEW(s)                                                                \
    ({                                                                         \
        __auto_type _sc = (s);                                                 \
        VIEW_OF(_sc, _sc.len, 0);                                              \
    })

static struct code read_code(FILE *file) {
    struct code c = {0};
    while (true) {
        char ch = fgetc(file);
        assert(!feof(file) && "EOF in code");

        if (c.len == c.cap) {
            c.cap = 2 * (c.cap ?: 4);
            c.keys = realloc(c.keys, c.cap * sizeof(*c.keys));
            assert(c.keys != NULL && "Out of memory");
        }
        c.keys[c.len++] = ch;

        if (ch == 'A') {
            break;
        }
    }
    return c;
}

static inline bool pos_equal(struct pos a, struct pos b) {
    return a.r == b.r && a.c == b.c;
}

static inline struct offset diff_btn(struct pos p1, struct pos p2) {
    return (struct offset){.dr = p1.r - p2.r, .dc = p1.c - p2.c};
}

static inline struct pos offset(struct pos start, struct offset off) {
    return (struct pos){.r = start.r + off.dr, .c = start.c + off.dc};
}

static struct offset unitize(struct offset off, bool vert) {
    if (vert) {
        assert(off.dr != 0 &&
               "Unitizing in vert direction with no vert component");
        return (struct offset){.dr = off.dr < 0 ? -1 : 1, .dc = 0};
    } else {
        assert(off.dc != 0 &&
               "Unitizing in hori direction with no hori component");
        return (struct offset){.dr = 0, .dc = off.dc < 0 ? -1 : 1};
    }
}

static enum arrpad_key arrpad_from_ch(char ch) {
    switch (ch) {
    case '^':
        return AP_U;
    case 'v':
        return AP_D;
    case '<':
        return AP_L;
    case '>':
        return AP_R;
    case 'A':
        return AP_A;
    default:
        fprintf(stderr, "Invalid key for Arrow Pad\n");
        exit(1);
    }
}

static ulong code_as_num(struct code_view c) {
    ulong res = 0;
    ulong ind = 0;
    while (ind < c.len && isdigit(c.keys[ind])) {
        res = (10 * res) + (c.keys[ind] - '0');
        ++ind;
    }
    return res;
}

static struct pos get_pos_for_num(struct code_view to_type) {
    assert(to_type.len > 0);
    char c = to_type.keys[0];
    enum numpad_key k = c == 'A' ? NP_A : (c - '0' + NP_0);
    return numpad_posns[k];
}

static struct pos get_pos_for_arr(struct code_view to_type) {
    assert(to_type.len > 0);
    return arrpad_posns[arrpad_from_ch(to_type.keys[0])];
}

static ulong cached_step_arr(struct pos initial, struct pos target,
                             struct code building, int remaining);

static ulong _step_arr(struct pos initial, struct pos target,
                       struct code building, int remaining) {
    if (pos_equal(initial, target)) {
        struct code copy = building;
        assert(copy.len + 1 < copy.cap);
        copy.keys[copy.len++] = 'A';

        if (remaining == 0) {
            return copy.len;
        } else {
            char arr_keys[INLINE_BUF] = {0};
            struct code arr_buf = {
                .keys = arr_keys, .len = 0, .cap = INLINE_BUF};
            struct code_view built_view = VIEW(copy);
            struct pos arr_initial = arrpad_posns[AP_A];

            ulong min = 0;
            while (built_view.len > 0) {
                struct pos arr_target = get_pos_for_arr(built_view);
                built_view = VIEW_OF_TAIL(built_view, 1);
                min += cached_step_arr(arr_initial, arr_target, arr_buf,
                                       remaining - 1);
                arr_initial = arr_target;
                arr_buf.len = 0;
            }
            return min;
        }
    } else {
        struct offset diff = diff_btn(target, initial);

        ulong test_min = ULONG_MAX;
        if (diff.dr != 0) {
            struct offset vert = unitize(diff, true);
            struct pos stepped = offset(initial, vert);

            if (!pos_equal(stepped, invalid_arr)) {
                char ch = vert.dr < 0 ? '^' : 'v';

                struct code copy = building;
                assert(copy.len + 1 < copy.cap);
                copy.keys[copy.len++] = ch;

                ulong res = _step_arr(stepped, target, copy, remaining);
                if (res < test_min) {
                    test_min = res;
                }
            }
        }

        if (diff.dc != 0) {
            struct offset hori = unitize(diff, false);
            struct pos stepped = offset(initial, hori);

            if (!pos_equal(stepped, invalid_arr)) {
                char ch = hori.dc < 0 ? '<' : '>';

                struct code copy = building;
                assert(copy.len + 1 < copy.cap);
                copy.keys[copy.len++] = ch;

                ulong res = _step_arr(stepped, target, copy, remaining);
                if (res < test_min) {
                    test_min = res;
                }
            }
        }
        return test_min;
    }
}

static ulong get_key(struct pos initial, struct pos target, int remaining) {
    ulong key = 0;

    key = (ARR_PAD_ROWS * key) + initial.r;
    key = (ARR_PAD_COLS * key) + initial.c;
    key = (ARR_PAD_ROWS * key) + target.r;
    key = (ARR_PAD_COLS * key) + target.c;
    key = (MAX_RECUR * key) + (MAX_RECUR - 1 - remaining);

    return key;
}

static ulong cached_step_arr(struct pos initial, struct pos target,
                             struct code building, int remaining) {
    struct entry {
        ulong key;
        ulong val;
    };

    struct bitree_node {
        struct bitree_node *l;
        struct bitree_node *r;
        struct entry entry;
    };

    struct bitree {
        struct bitree_node *root;
    };

    static struct bitree tree = {0};

    ulong key = get_key(initial, target, remaining);

    struct bitree_node **cur = &tree.root;
    while (*cur != NULL) {
        ulong cur_key = (*cur)->entry.key;
        if (key == cur_key) {
            break;
        }

        if (key < cur_key) {
            cur = &(*cur)->l;
        } else {
            cur = &(*cur)->r;
        }
    }

    if (*cur == NULL) {
        ulong val = _step_arr(initial, target, building, remaining);

        struct bitree_node *new_node = malloc(sizeof(*new_node));
        *new_node = (struct bitree_node){
            .l = NULL,
            .r = NULL,
            .entry = {.key = key, .val = val},
        };

        *cur = new_node;
    }
    return (*cur)->entry.val;
}

static ulong step_num(struct pos initial, struct pos target,
                      struct code building, int remaining) {
    if (pos_equal(initial, target)) {
        struct code copy = building;
        assert(copy.len + 1 < copy.cap);
        copy.keys[copy.len++] = 'A';

        char arr_keys[INLINE_BUF] = {0};
        struct code arr_buf = {.keys = arr_keys, .len = 0, .cap = INLINE_BUF};
        struct code_view built_view = VIEW(copy);
        struct pos arr_initial = arrpad_posns[AP_A];

        ulong min = 0;
        while (built_view.len > 0) {
            struct pos arr_target = get_pos_for_arr(built_view);
            built_view = VIEW_OF_TAIL(built_view, 1);
            min += cached_step_arr(arr_initial, arr_target, arr_buf, remaining);
            arr_initial = arr_target;
            arr_buf.len = 0;
        }
        return min;
    } else {
        struct offset diff = diff_btn(target, initial);

        ulong test_min = ULONG_MAX;
        if (diff.dr != 0) {
            struct offset vert = unitize(diff, true);
            struct pos stepped = offset(initial, vert);

            if (!pos_equal(stepped, invalid_num)) {
                char ch = vert.dr < 0 ? '^' : 'v';

                struct code copy = building;
                assert(copy.len + 1 < copy.cap);
                copy.keys[copy.len++] = ch;

                ulong res = step_num(stepped, target, copy, remaining);
                if (res < test_min) {
                    test_min = res;
                }
            }
        }

        if (diff.dc != 0) {
            struct offset hori = unitize(diff, false);
            struct pos stepped = offset(initial, hori);

            if (!pos_equal(stepped, invalid_num)) {
                char ch = hori.dc < 0 ? '<' : '>';

                struct code copy = building;
                assert(copy.len + 1 < copy.cap);
                copy.keys[copy.len++] = ch;

                ulong res = step_num(stepped, target, copy, remaining);
                if (res < test_min) {
                    test_min = res;
                }
            }
        }
        return test_min;
    }
}

static void partOne(FILE *file) {
    ulong total = 0;
    while (true) {
        char ch = fgetc(file);
        if (feof(file)) {
            break;
        }
        ungetc(ch, file);

        struct code c = read_code(file);
        assert((fgetc(file) == '\n') && "Expected EOL");

        char num_keys[INLINE_BUF] = {0};
        struct code num_buf = {.keys = num_keys, .len = 0, .cap = INLINE_BUF};
        struct code_view view = VIEW(c);
        struct pos initial = numpad_posns[NP_A];

        ulong shortest = 0;
        while (view.len > 0) {
            struct pos target = get_pos_for_num(view);
            view = VIEW_OF_TAIL(view, 1);
            shortest += step_num(initial, target, num_buf, 1);
            initial = target;
            num_buf.len = 0;
        }

        ulong code_num = code_as_num(VIEW(c));
        fprintf(stdout, "Code: %.*s; num = %lu; count = %lu\n", (int)c.len,
                c.keys, code_num, shortest);

        total += code_num * shortest;
    }
    fprintf(stdout, "Result = %lu\n", total);
}

static void partTwo(FILE *file) {
    ulong total = 0;
    while (true) {
        char ch = fgetc(file);
        if (feof(file)) {
            break;
        }
        ungetc(ch, file);

        struct code c = read_code(file);
        assert((fgetc(file) == '\n') && "Expected EOL");

        char num_keys[INLINE_BUF] = {0};
        struct code num_buf = {.keys = num_keys, .len = 0, .cap = INLINE_BUF};
        struct code_view view = VIEW(c);
        struct pos initial = numpad_posns[NP_A];

        ulong shortest = 0;
        while (view.len > 0) {
            struct pos target = get_pos_for_num(view);
            view = VIEW_OF_TAIL(view, 1);
            shortest += step_num(initial, target, num_buf, MAX_RECUR - 1);
            initial = target;
            num_buf.len = 0;
        }

        ulong code_num = code_as_num(VIEW(c));
        fprintf(stdout, "Code: %.*s; num = %lu; count = %lu\n", (int)c.len,
                c.keys, code_num, shortest);

        total += code_num * shortest;
    }
    fprintf(stdout, "Result = %lu\n", total);
}

int main(int argc, char const *argv[]) {
    assert(argc > 1);

    char const *filename = argv[1];
    FILE *file = fopen(filename, "r");
    fseek(file, 0, SEEK_SET);

    int part = 1;
    if (argc > 2) {
        part = atoi(argv[2]);
    }

    switch (part) {
    case 1:
        partOne(file);
        break;
    case 2:
        partTwo(file);
        break;
    default:
        fprintf(stderr, "Invalid part\n");
        exit(1);
    }
}
