#include <assert.h>
#include <ctype.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/types.h>
#include <time.h>

#define MIX(a, b) (a ^ b)
#define PRUNE(n) ((n) & ((1 << 24) - 1))

#ifndef STEPS
#define STEPS 2000
#endif

static ulong next_secret(ulong secret) {
    secret = PRUNE(MIX((secret << 6), secret));
    secret = PRUNE(MIX((secret >> 5), secret));
    secret = PRUNE(MIX((secret << 11), secret));
    return secret;
}

static ulong read_number(FILE *file) {
    ulong res = 0;
    while (true) {
        char c = fgetc(file);
        if (c == '\n')
            break;

        assert(isdigit(c));

        res = (10 * res) + (c - '0');
    }
    return res;
}

static void partOne(FILE *file) {
    ulong total = 0;
    while (true) {
        char c = fgetc(file);
        if (feof(file)) {
            break;
        }
        ungetc(c, file);

        ulong secret = read_number(file);
        for (int i = 0; i < STEPS; ++i) {
            secret = next_secret(secret);
        }

        total += secret;
    }
    printf("Total = %lu\n", total);
}

struct wrapped {
    short *vals;
    short cap;
    short ind;
};

static void add_short(struct wrapped *shorts, short c) {
    shorts->vals[shorts->ind] = c;
    shorts->ind = (shorts->ind + 1) % shorts->cap;
}

static short back(struct wrapped const shorts, short ind) {
    assert(ind < shorts.cap);

    short real_ind = (shorts.ind + shorts.cap - (ind + 1)) % shorts.cap;
    return shorts.vals[real_ind];
}

static bool w_equal(struct wrapped const c1, struct wrapped const c2) {
    assert(c1.cap == c2.cap);

    short i1 = 0;
    short i2 = 0;

    while (i1 < c1.cap) {
        short v1 = c1.vals[(i1 + c1.ind) % c1.cap];
        short v2 = c2.vals[(i2 + c2.ind) % c1.cap];

        if (v1 != v2) {
            return false;
        }

        ++i1;
        ++i2;
    }

    return true;
}

struct entry {
    ulong key;
    ulong val;
};

struct bi_tree_node {
    struct bi_tree_node *l;
    struct bi_tree_node *r;
    struct entry entry;
};

struct bi_tree {
    struct bi_tree_node *root;
};

static ulong get_key(struct wrapped shorts) {
    ulong key = 0;

    short cur_ind = shorts.ind;
    do {
        assert(shorts.vals[cur_ind] < 20);
        key = (20 * key) + (shorts.vals[cur_ind] + 10);
        cur_ind = (cur_ind + 1) % shorts.cap;
    } while (cur_ind != shorts.ind);

    assert(key != 0);
    return key;
}

struct bi_tree_node **get_node(struct bi_tree *tree, struct wrapped w) {
    ulong key = get_key(w);

    struct bi_tree_node **res = &tree->root;
    while (*res != NULL) {
        ulong cur_key = (*res)->entry.key;

        if (key == cur_key) {
            break;
        }

        if (key < cur_key) {
            res = &(*res)->l;
        } else {
            res = &(*res)->r;
        }
    }

    return res;
}

struct num_list {
    ulong *secrets;
    ulong len;
    ulong cap;
};

static ulong score_for(struct num_list l, struct wrapped const target_diffs) {
    ulong total = 0;

    for (ulong num_i = 0; num_i < l.len; ++num_i) {
        ulong secret = l.secrets[num_i];

        short shorts_buf[5];
        struct wrapped shorts = {.vals = shorts_buf, .cap = 5, .ind = 0};

        short diffs_buf[4];
        struct wrapped diffs = {.vals = diffs_buf, .cap = 4, .ind = 0};

        add_short(&shorts, secret % 10);
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        ulong i = 4;
        do {
            if (w_equal(diffs, target_diffs)) {
                // printf("For number %lu, adding %d\n", l.secrets[num_i],
                //        back(shorts, 0));

                total += back(shorts, 0);
                break;
            }

            add_short(&shorts, secret % 10);
            add_short(&diffs, back(shorts, 0) - back(shorts, 1));
            secret = next_secret(secret);

            ++i;
        } while (i <= STEPS);
    }

    return total;
}

static ulong get_max_subtree(struct bi_tree_node cur) {
    ulong max = cur.entry.val;
    if (cur.l != NULL) {
        ulong smax = get_max_subtree(*cur.l);
        if (smax > max) {
            max = smax;
        }
    }
    if (cur.r != NULL) {
        ulong smax = get_max_subtree(*cur.r);
        if (smax > max) {
            max = smax;
        }
    }
    return max;
}

static void partTwo(FILE *file) {
    struct num_list l = {0};
    while (true) {
        char c = fgetc(file);
        if (feof(file)) {
            break;
        }
        ungetc(c, file);

        ulong secret = read_number(file);
        if (l.len == l.cap) {
            l.cap = 2 * (l.cap ?: 4);
            l.secrets = realloc(l.secrets, l.cap * sizeof(*l.secrets));
            assert(l.secrets != NULL);
        }
        l.secrets[l.len++] = secret;
    }

    // short diffs[4] = {0};
    // struct wrapped test_diff = {.vals = diffs, .cap = 4, .ind = 0};
    // add_short(&test_diff, -9);
    // add_short(&test_diff, -9);
    // add_short(&test_diff, -9);
    // add_short(&test_diff, -9);
    // fprintf(stdout, "Test diff = %lu\n", score_for(l, test_diff));

    // return;

    struct bi_tree tree = {0};

    clock_t t_start = clock();
    for (ulong num_i = 0; num_i < l.len; ++num_i) {
        clock_t c_start = clock();
        printf("%lu/%lu", num_i, l.len);

        ulong secret = l.secrets[num_i];

        short shorts_buf[5];
        struct wrapped shorts = {.vals = shorts_buf, .cap = 5, .ind = 0};

        short diffs_buf[4];
        struct wrapped diffs = {.vals = diffs_buf, .cap = 4, .ind = 0};

        add_short(&shorts, secret % 10);
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        add_short(&shorts, secret % 10);
        add_short(&diffs, back(shorts, 0) - back(shorts, 1));
        secret = next_secret(secret);

        ulong i = 4;
        do {
            struct bi_tree_node **entry = get_node(&tree, diffs);
            if (*entry == NULL) {
                ulong val = score_for(l, diffs);

                struct bi_tree_node *new_node = malloc(sizeof(*new_node));
                *new_node = (struct bi_tree_node){
                    .l = NULL,
                    .r = NULL,
                    .entry = {.key = get_key(diffs), .val = val},
                };

                *entry = new_node;
            }

            add_short(&shorts, secret % 10);
            add_short(&diffs, back(shorts, 0) - back(shorts, 1));
            secret = next_secret(secret);

            ++i;
        } while (i <= STEPS);

        clock_t c_end = clock();
        printf(", iter = %f seconds\n",
               ((double)(c_end - c_start) / CLOCKS_PER_SEC));
    }
    clock_t t_end = clock();
    printf("Full = %f seconds\n", ((double)(t_end - t_start) / CLOCKS_PER_SEC));

    if (tree.root != NULL) {
        ulong max = get_max_subtree(*tree.root);
        printf("Max bananas = %lu\n", max);
    } else {
        printf("No bananas\n");
    }
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
