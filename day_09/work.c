#include <assert.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

typedef unsigned long ulong;
#define EMPTY ((ulong)(-1))

struct file_system {
    ulong *contents;
    ulong length;
    ulong capacity;
};

static void append_nums(struct file_system *fs, ulong n, ulong count) {
    assert(fs != NULL);

    ulong target_len = fs->length + count;
    if (fs->capacity < target_len) {
        ulong next_cap = fs->capacity ?: 8;
        do {
            next_cap *= 2;
        } while (next_cap < target_len);

        ulong *next_contents = realloc(fs->contents, next_cap * sizeof(ulong));
        assert(next_contents);

        fs->contents = next_contents;
        fs->capacity = next_cap;
    }

    ulong i = 0;
    while (i++ < count) {
        fs->contents[fs->length++] = n;
    }
}

void part_one(FILE *file) {
    bool is_file = true;
    ulong file_index = 0;
    ulong total_files = 0;

    struct file_system files = {
        .contents = NULL,
        .length = 0,
        .capacity = 0,
    };

    while (true) {
        assert(file_index != EMPTY);

        char c = fgetc(file);
        if (feof(file) || !('0' <= c && c <= '9'))
            break;

        ulong count = c - '0';
        if (is_file)
            total_files += count;

        append_nums(&files, is_file ? file_index++ : EMPTY, count);
        is_file = !is_file;
    }

#if _DEBUG
    for (ulong i = 0; i < files.length; ++i) {
        ulong fi = files.contents[i];
        if (fi < 10)
            printf("%c", (char)(fi + '0'));
        else if (fi == EMPTY)
            printf(".");
        else
            printf("x");
    }
    printf("\n");
#endif

    ulong result = 0;
    ulong *head = files.contents;
    ulong *tail = files.contents + files.length;

    ulong i = 0;
    while (i < total_files) {
        ulong to_add = *head++;
        while (to_add == EMPTY) {
            to_add = *--tail;
        }

#if _DEBUG
        if (to_add < 10)
            printf("%c", (char)(to_add + '0'));
#endif

        result += i++ * to_add;
    }
#if _DEBUG
    while (i++ < files.length)
        printf(".");
#endif

    printf("\nMax long: %lu", EMPTY);
    printf("\n======\nResult: %lu\n", result);
}

void part_two(FILE *file) {
    struct file_system files = {
        .contents = NULL,
        .length = 0,
        .capacity = 0,
    };

    while (true) {
        char c = fgetc(file);
        if (feof(file) || !('0' <= c && c <= '9'))
            break;

        append_nums(&files, c - '0', 1);
    }

    ulong *head = files.contents;
    ulong *tail = files.contents + files.length;
    ulong last_fill_ind = files.length - 1 - ((files.length + 1) % 2);

    ulong res = 0;
    ulong ind = 0;

    for (ulong i = 0; i < files.length; ++i) {
        if (i % 2 == 0) {
            ulong val = files.contents[i];
            if (((long)val) < 0) {
                long nlval = -(long)val;
                ind += nlval;
                continue;
            }
            // even, run the numbers
            for (ulong j = 0; j < val; ++j) {
                res += ind * (i / 2);
                ind += 1;
            }
        } else {
            // try to fill from the end
            ulong holes = files.contents[i];
            ulong l = last_fill_ind;
            while (holes > 0 && l > i && l < files.length) {
                ulong l_count = files.contents[l];
                if (l_count <= holes) {
                    holes -= l_count;
                    files.contents[l] -= 2 * l_count; // make it equal -l_count
                    for (ulong j = 0; j < l_count; ++j) {
                        res += ind * (l / 2);
                        ind += 1;
                    }
                }
                l -= 2;
            }
            ind += holes;
        }
    }
    printf("Result: %lu\n", res);
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

    fseek(file, 0, SEEK_SET);

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
