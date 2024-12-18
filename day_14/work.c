#include <alloca.h>
#include <assert.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <string.h>

static bool read_robot(FILE *file, long *x, long *y, long *dx, long *dy) {
    char c = fgetc(file);
    if (feof(file)) {
        return false;
    }

    assert(c == 'p');
    assert(fgetc(file) == '=');

    long x_buf = 0;
    while (true) {
        char d = fgetc(file);
        if (d == ',') {
            break;
        }
        x_buf = (10 * x_buf) + (d - '0');
    }

    long y_buf = 0;
    while (true) {
        char d = fgetc(file);
        if (d == ' ') {
            break;
        }
        y_buf = (10 * y_buf) + (d - '0');
    }

    assert(fgetc(file) == 'v');
    assert(fgetc(file) == '=');

    long dx_buf = 0;
    {
        char d = fgetc(file);
        bool neg_dx = false;
        if (d == '-') {
            neg_dx = true;
            d = fgetc(file);
        }
        while (d != ',') {
            dx_buf = (10 * dx_buf) + (d - '0');
            d = fgetc(file);
        }

        if (neg_dx) {
            dx_buf *= -1;
        }
    }

    long dy_buf = 0;
    {
        char d = fgetc(file);
        bool neg_dy = false;
        if (d == '-') {
            neg_dy = true;
            d = fgetc(file);
        }
        while (d != '\n') {
            dy_buf = (10 * dy_buf) + (d - '0');
            d = fgetc(file);
        }

        if (neg_dy) {
            dy_buf *= -1;
        }
    }

    *x = x_buf;
    *y = y_buf;
    *dx = dx_buf;
    *dy = dy_buf;

    return true;
}

static void step_robot(long *x, long *y, long dx, long dy, long width,
                       long height) {
    *x = (((*x + dx) % width) + width) % width;
    *y = (((*y + dy) % height) + height) % height;
}

static long get_quad(long x, long y, long width, long height) {
    assert(width % 2 == 1);
    assert(height % 2 == 1);
    if (((2 * x + 1) == width) || ((2 * y + 1) == height)) {
        return -1;
    }

    long x_off = ((2 * x + 1) < width) ? 0 : 1;
    long y_off = ((2 * y + 1) < height) ? 0 : 1;
    return 2 * x_off + y_off;
}

static void partOne(FILE *file, long width, long height, long secs) {
    long quads[4] = {0};

    while (true) {
        long x, y, dx, dy;
        if (!read_robot(file, &x, &y, &dx, &dy)) {
            break;
        }

        for (long i = 0; i < secs; ++i) {
            step_robot(&x, &y, dx, dy, width, height);
        }

        long q = get_quad(x, y, width, height);
        if (q >= 0) {
            quads[q] += 1;
        }
    }

    printf("Score: %lu\n", (quads[0] * quads[1] * quads[2] * quads[3]));
}

static void fprint_robots(FILE *out, long *robots, long len, long width,
                          long height) {
    char *buf = alloca(width * height);
    memset(buf, '.', width * height);
    for (long i = 0; i < len; ++i) {
        buf[width * robots[4 * i + 1] + robots[4 * i + 0]] = '*';
    }

    for (long i = 0; i < height; ++i) {
        fprintf(out, "%.*s\n", (int)width, buf + width * i);
    }
}

static void partTwo(FILE *file, long width, long height) {
    long len = 0;
    long cap = 8;
    long *robots = malloc(cap * sizeof(long[4]));
    assert(robots != NULL);

    long *cols = malloc(width * sizeof(long));

    while (true) {
        long x, y, dx, dy;
        if (!read_robot(file, &x, &y, &dx, &dy)) {
            break;
        }

        if (len == cap) {
            cap *= 2;
            robots = realloc(robots, cap * sizeof(long[4]));
            assert(robots != NULL);
        }

        robots[4 * len + 0] = x;
        robots[4 * len + 1] = y;
        robots[4 * len + 2] = dx;
        robots[4 * len + 3] = dy;

        ++len;
    }

    FILE *out = fopen("outputs.txt", "a");

    long count = 0;
    while (count < 1e4) {
        ++count;

        if ((count % 100000) == 0) {
            printf("count: %ld\n", count);
        }

        for (long i = 0; i < len; ++i) {
            long *rob = robots + 4 * i;
            step_robot(rob + 0, rob + 1, rob[2], rob[3], width, height);
        }

        fprintf(out, "Iteration %ld\n", count);
        fprint_robots(out, robots, len, width, height);
    }

    fclose(out);
    printf("Look in the file \"outputs.txt\" to find the tree\n");
}

int main(int argc, char const *argv[]) {
    assert(argc > 3);

    long width = atol(argv[1]);
    long height = atol(argv[2]);
    long secs = 100;

    char const *filename = argv[3];
    int part = 1;
    if (argc > 4) {
        part = atoi(argv[4]);
    }

    FILE *file = fopen(filename, "r");
    fseek(file, 0, SEEK_SET);

    switch (part) {
    case 1:
        partOne(file, width, height, secs);
        break;
    case 2:
        partTwo(file, width, height);
        break;
    default:
        fprintf(stderr, "Invalid part\n");
        exit(1);
    }
    fclose(file);
}
