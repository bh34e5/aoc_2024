#include <assert.h>
#include <ctype.h>
#include <math.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

#ifdef _DEBUG
#define ASSERT(...) assert(__VA_ARGS__)
#else
#define ASSERT(...) (void)0
#endif

typedef unsigned long ulong;

struct dll_el {
    struct dll_el *next;
    struct dll_el *prev;
};

struct dll {
    struct dll_el *sentinel;
    ulong len;
};

struct dll new_dll() {
    struct dll_el *sentinel = malloc(sizeof(*sentinel));
    ASSERT(sentinel != NULL);

    sentinel->next = sentinel->prev = sentinel;
    return (struct dll){.sentinel = sentinel, .len = 0};
}

static void insert_at(struct dll *l, ulong ind, struct dll_el *el) {
    ASSERT(l != NULL);
    ASSERT(el != NULL);
    ASSERT(0 <= ind && ind <= l->len);

    struct dll_el *next;
    if (ind == l->len) {
        next = l->sentinel;
    } else {
        next = l->sentinel->next;
        while (ind-- > 0) {
            next = next->next;
        }
    }
    el->next = next;
    el->prev = next->prev;

    el->next->prev = el;
    el->prev->next = el;

    ++l->len;
}

static inline void insert_end(struct dll *l, struct dll_el *el) {
    ASSERT(l != NULL);
    ASSERT(el != NULL);

    insert_at(l, l->len, el);
}

static bool contains(struct dll *l, struct dll_el *el) {
    ASSERT(l != NULL);
    ASSERT(el != NULL);

    struct dll_el *c = l->sentinel->next;
    while (c != l->sentinel) {
        if (c == el)
            return true;

        c = c->next;
    }
    return false;
}

static void remove_el(struct dll *l, struct dll_el *el) {
    ASSERT(l != NULL);
    ASSERT(el != NULL);
    ASSERT(l->len > 0);
    ASSERT(contains(l, el));
    ASSERT(el != l->sentinel);

    struct dll_el *p = el->prev;
    struct dll_el *n = el->next;

    p->next = n;
    n->prev = p;

    --l->len;
}

static struct dll_el *get_at(struct dll l, ulong ind) {
    ASSERT(l.len > 0);
    ASSERT(0 <= ind && ind < l.len);

    struct dll_el *next = l.sentinel->next;
    while (ind-- > 0) {
        next = next->next;
    }
    return next;
}

struct num_el {
    struct dll_el header;
    ulong num;
    ulong count; // unused in part 1
};

struct dll read_num_list(FILE *file) {
    struct dll res = new_dll();

    bool digit = false;
    ulong cur = 0;

    bool cont = true;
    while (cont) {
        char c = fgetc(file);
        if (feof(file)) {
            cont = false;
            goto finish_num;
        }

        if (isspace(c)) {
            goto finish_num;
        }

        ASSERT(isdigit(c));
        digit = true;
        cur = (10 * cur) + (c - '0');

        continue;

    finish_num:
        if (digit) {
            struct num_el *num = malloc(sizeof(*num));
            ASSERT(num != NULL);

            num->num = cur;
            num->count = 1;
            insert_end(&res, &num->header);

            digit = false;
            cur = 0;
        }
    }

    return res;
}

static inline unsigned int num_digits(ulong n) {
    return n == 0 ? 1 : (unsigned int)floor(log10(n) + 1.0);
}

static void blink(struct dll *nums) {
    ulong ind = 0;
    struct dll_el *el = nums->sentinel->next;

    while (el != nums->sentinel) {
        struct num_el *nel = (struct num_el *)el;
        ulong l = nel->num;

        if (l == 0) {
            nel->num = 1;
        } else {
            unsigned int nd = num_digits(l);
            if (nd % 2 == 0) {
                struct num_el *pair = malloc(sizeof(*pair));
                ulong ten_pow = pow(10, (double)nd / 2);
                ulong lower = l % ten_pow;
                ulong upper = (l - lower) / ten_pow;

                pair->count = nel->count;

                pair->num = upper;
                nel->num = lower;

                insert_at(nums, ind, &pair->header);

                // increment ind since we just added something before us
                ind += 1;
            } else {
                nel->num *= 2024;
            }
        }

        ind += 1;
        el = el->next;
    }
}

void part_one(FILE *file) {
    struct dll nums = read_num_list(file);

    ulong num_blinks = 25;
    for (ulong i = 0; i < num_blinks; ++i) {
        blink(&nums);
    }

#if _DEBUG
    struct dll_el *el = nums.sentinel->next;
    while (el != nums.sentinel) {
        ulong l = ((struct num_el *)el)->num;
        printf("%lu ", l);
        el = el->next;
    }
    printf("\n");
#endif

    printf("Num elements: %lu\n", nums.len);
}

static void consolidate(struct dll *nums) {
    ulong ind = 0;
    struct dll_el *el = nums->sentinel->next;
    while (el != nums->sentinel) {
        struct num_el *nel = (struct num_el *)el;
        ulong cur_num = nel->num;

        struct dll_el *other = el->next;
        while (other != nums->sentinel) {
            struct num_el *nother = (struct num_el *)other;
            if (nother->num == cur_num) {
                struct dll_el *to_remove = other;
                nel->count += nother->count;
                other = other->next;

                remove_el(nums, to_remove);
                free(to_remove);
                // make sure all the pointers are correct
                el = get_at(*nums, ind);
                nel = (struct num_el *)el;
            } else {
                other = other->next;
            }
        }
        el = el->next;
        ind += 1;
    }
}

void part_two(FILE *file) {
    struct dll nums = read_num_list(file);

    ulong num_blinks = 75;
    for (ulong i = 0; i < num_blinks; ++i) {
        if (((i + 1) % 5) == 0) {
            printf("%lu ", i + 1);
            fflush(stdout);
        }
        blink(&nums);
        consolidate(&nums);
    }
    printf("\n");

    ulong total = 0;
    struct dll_el *el = nums.sentinel->next;
    while (el != nums.sentinel) {
        struct num_el *nel = (struct num_el *)el;
        ulong l = nel->num;
        total += nel->count;

#if _DEBUG
        printf("%lu ", l);
#endif

        el = el->next;
    }

#if _DEBUG
    printf("\n");
#endif

    printf("Num elements: %lu (len of array %lu)\n", total, nums.len);
}

int main(int argc, char const *argv[]) {
    char const *filename = argv[1];
    FILE *file = fopen(filename, "r");
    if (file == NULL) {
        fprintf(stderr, "Could not open file (%s)\n", filename);
        exit(1);
    }

    int part = 1;
    if (argc > 2) {
        part = atoi(argv[2]);
    }

    switch (part) {
    case 1:
        part_one(file);
        break;
    case 2:
        part_two(file);
        break;
    default:
        fprintf(stderr, "Invalid part: %d\n", part);
        exit(1);
    }
}
