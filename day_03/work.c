#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>

enum read_state_one {
    RS_,
    RS_M,
    RS_MU,
    RS_MUL,
    RS_MUL_OPEN,
    RS_MUL_OPEN_NUM,
    RS_MUL_OPEN_NUM_COMMA,
    RS_MUL_OPEN_NUM_COMMA_NUM,
};

void part_one(FILE *file) {
    fprintf(stdout, "Part one\n");

    fseek(file, 0, SEEK_SET);

    int res = 0;
    int num_one = 0;
    int num_two = 0;

    enum read_state_one state = RS_;
    do {
        char c = fgetc(file);
        if (feof(file)) {
            break;
        }

        switch (state) {
        case RS_: {
            if (c == 'm') {
                state = RS_M;
            } else {
                goto reset;
            }
            break;
        }
        case RS_M: {
            if (c == 'u') {
                state = RS_MU;
            } else {
                goto reset;
            }
            break;
        }
        case RS_MU: {
            if (c == 'l') {
                state = RS_MUL;
            } else {
                goto reset;
            }
            break;
        }
        case RS_MUL: {
            if (c == '(') {
                state = RS_MUL_OPEN;
            } else {
                goto reset;
            }
            break;
        }
        case RS_MUL_OPEN: {
            if (!(('0' <= c) && (c <= '9'))) {
                goto reset;
            }
            state = RS_MUL_OPEN_NUM;
            // fall through
        }
        case RS_MUL_OPEN_NUM: {
            if (('0' <= c) && (c <= '9')) {
                num_one = (10 * num_one) + (c - '0');
            } else if (c == ',') {
                state = RS_MUL_OPEN_NUM_COMMA;
            } else {
                goto reset;
            }
            break;
        }
        case RS_MUL_OPEN_NUM_COMMA: {
            if (!(('0' <= c) && (c <= '9'))) {
                goto reset;
            }
            state = RS_MUL_OPEN_NUM_COMMA_NUM;
            // fall through
        }
        case RS_MUL_OPEN_NUM_COMMA_NUM: {
            if (('0' <= c) && (c <= '9')) {
                num_two = (10 * num_two) + (c - '0');
            } else if (c == ')') {
                res += num_one * num_two;
                goto reset;
            } else {
                goto reset;
            }
            break;
        }
        }

        continue;

    reset:
        state = RS_;
        num_one = 0;
        num_two = 0;
    } while (true);

    fprintf(stdout, "Sum: %d\n", res);
}

enum read_state_two {
    RRS_,
    RRS_M,
    RRS_MU,
    RRS_MUL,
    RRS_MUL_OPEN,
    RRS_MUL_OPEN_NUM,
    RRS_MUL_OPEN_NUM_COMMA,
    RRS_MUL_OPEN_NUM_COMMA_NUM,
    RRS_D,
    RRS_DO,
    RRS_DO_OPEN,
    RRS_DON,
    RRS_DON_APO,
    RRS_DON_APO_T,
    RRS_DON_APO_T_OPEN,
};

void part_two(FILE *file) {
    fprintf(stdout, "Part two\n");

    fseek(file, 0, SEEK_SET);

    bool enabled = true;
    int res = 0;
    int num_one = 0;
    int num_two = 0;

    enum read_state_two state = RRS_;
    do {
        char c = fgetc(file);
        if (feof(file)) {
            break;
        }

        switch (state) {
        case RRS_: {
            if (c == 'm') {
                state = RRS_M;
            } else if (c == 'd') {
                state = RRS_D;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_M: {
            if (c == 'u') {
                state = RRS_MU;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_MU: {
            if (c == 'l') {
                state = RRS_MUL;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_MUL: {
            if (c == '(') {
                state = RRS_MUL_OPEN;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_MUL_OPEN: {
            if (!(('0' <= c) && (c <= '9'))) {
                goto reset;
            }
            state = RRS_MUL_OPEN_NUM;
            // fall through
        }
        case RRS_MUL_OPEN_NUM: {
            if (('0' <= c) && (c <= '9')) {
                num_one = (10 * num_one) + (c - '0');
            } else if (c == ',') {
                state = RRS_MUL_OPEN_NUM_COMMA;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_MUL_OPEN_NUM_COMMA: {
            if (!(('0' <= c) && (c <= '9'))) {
                goto reset;
            }
            state = RRS_MUL_OPEN_NUM_COMMA_NUM;
            // fall through
        }
        case RRS_MUL_OPEN_NUM_COMMA_NUM: {
            if (('0' <= c) && (c <= '9')) {
                num_two = (10 * num_two) + (c - '0');
            } else if (c == ')') {
                if (enabled) {
                    res += num_one * num_two;
                }
                goto reset;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_D: {
            if (c == 'o') {
                state = RRS_DO;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DO: {
            if (c == '(') {
                state = RRS_DO_OPEN;
            } else if (c == 'n') {
                state = RRS_DON;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DO_OPEN: {
            if (c == ')') {
                enabled = true;
                goto reset;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DON: {
            if (c == '\'') {
                state = RRS_DON_APO;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DON_APO: {
            if (c == 't') {
                state = RRS_DON_APO_T;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DON_APO_T: {
            if (c == '(') {
                state = RRS_DON_APO_T_OPEN;
            } else {
                goto reset;
            }
            break;
        }
        case RRS_DON_APO_T_OPEN: {
            if (c == ')') {
                enabled = false;
                goto reset;
            } else {
                goto reset;
            }
            break;
        }
        }

        continue;

    reset:
        state = RRS_;
        num_one = 0;
        num_two = 0;
    } while (true);

    fprintf(stdout, "Sum: %d\n", res);
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
