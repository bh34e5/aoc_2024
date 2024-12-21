#include <assert.h>
#include <ctype.h>
#include <errno.h>
#include <fcntl.h>
#include <pthread.h>
#include <stdbool.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>

typedef unsigned long ulong;
typedef unsigned char byte;

struct machine {
    union {
        struct {
            ulong reg_a;
            ulong reg_b;
            ulong reg_c;
        };
        ulong regs[3];
    };
    ulong ip;

    bool did_output;
    byte *program;
    ulong program_len;
    ulong program_cap;
};

static inline void assert_read(FILE *file, char const *str) {
    while (*str != '\0') {
        char c = fgetc(file);
        assert(!feof(file) && "Unexpected EOF");
        assert(c == *str && "Unexpected char");
        ++str;
    }
}

static void read_number(FILE *file, ulong *dest) {
    ulong local = *dest;
    while (true) {
        char c = fgetc(file);
        if (feof(file) || !isdigit(c)) {
            break;
        }

        local = (10 * local) + (c - '0');
    }
    *dest = local;
}

static void read_program(FILE *file, byte **prog_ptr, ulong *len_ptr,
                         ulong *cap_ptr) {
    ulong len = 0;
    ulong cap = 0;
    byte *prog = NULL;

    char c = ',';
    while (c == ',') {
        if (len == cap) {
            cap = 2 * (cap ?: 4);
            prog = realloc(prog, sizeof(*prog) * cap);
            assert(prog != NULL);
        }

        c = fgetc(file);
        prog[len++] = c - '0';

        c = fgetc(file);
    }

    *prog_ptr = prog;
    *len_ptr = len;
    *cap_ptr = cap;
}

static bool step_machine(struct machine *m, byte *out, int fd) {
    if (m->ip >= m->program_len) {
        return true; // halt
    }
    assert((m->ip + 1) < m->program_len && "Inst with no argument");

    byte inst = m->program[m->ip++];
    byte op = m->program[m->ip++];

#define LITERAL op
#define COMBO                                                                  \
    ({                                                                         \
        assert(op != 0b111 && "Invalid combo operand");                        \
        (op & 0b100) ? m->regs[(op & 0b11)] : op;                              \
    })

    switch (inst) {
    case 0: {
        m->reg_a = m->reg_a >> COMBO;
    } break;
    case 1: {
        m->reg_b = m->reg_b ^ LITERAL;
    } break;
    case 2: {
        m->reg_b = COMBO & 0b111;
    } break;
    case 3: {
        if (m->reg_a != 0) {
            m->ip = LITERAL;
        }
    } break;
    case 4: {
        m->reg_b = m->reg_b ^ m->reg_c;
    } break;
    case 5: {
        if (out != NULL) {
            *out = COMBO & 0b111;
        } else {
            char buf[2];
            int msg_len = 0;
            if (m->did_output) {
                buf[msg_len++] = ',';
            }
            buf[msg_len++] = ((COMBO & 0b111) + '0');
            assert(msg_len == write(fd, buf, msg_len) && "Failed to write");

            m->did_output = 1;
        }
    } break;
    case 6: {
        m->reg_b = m->reg_a >> COMBO;
    } break;
    case 7: {
        m->reg_c = m->reg_a >> COMBO;
    } break;
    default:
        assert(0 && "Invalid instruction");
    }

    return false;

#undef COMBO
#undef LITERAL
}

static struct machine read_machine(FILE *file) {
    struct machine m = {0};

    assert_read(file, "Register A: ");
    read_number(file, &m.reg_a);
    assert(!feof(file));

    assert_read(file, "Register B: ");
    read_number(file, &m.reg_b);
    assert(!feof(file));

    assert_read(file, "Register C: ");
    read_number(file, &m.reg_c);
    assert(!feof(file));

    assert(fgetc(file) == '\n');
    assert(!feof(file));

    assert_read(file, "Program: ");
    read_program(file, &m.program, &m.program_len, &m.program_cap);

    return m;
}

static void partOne(FILE *file) {
    struct machine m = read_machine(file);

    bool halt = false;
    while (!halt) {
        halt = step_machine(&m, NULL, fileno(stdout));
    }
    fprintf(stdout, "\n");
}

static bool try_number(struct machine m, ulong ind, ulong min, ulong *p_res) {
    byte target = m.program[ind];
    for (ulong a = min ?: 1; a < min + 8; ++a) {
        struct machine copy = m;
        copy.reg_a = a;

        byte out;
        bool halt = false;
        do {
            halt = step_machine(&copy, &out, -1);
        } while (!halt && copy.ip != 0);

        if (out == target) {
            if (ind == 0) {
                *p_res = a;
                return true;
            }
            ulong result;
            bool success = try_number(m, ind - 1, a << 3, &result);
            if (success) {
                *p_res = result;
                return true;
            }
        }
    }
    return false;
}

static void partTwo(FILE *file) {
    struct machine m = read_machine(file);

    ulong result;
    bool success = try_number(m, m.program_len - 1, 0, &result);

    if (!success) {
        printf("Not successful\n");
    }
    printf("Result = %lu\n", result);
}

int main(int argc, char const *argv[]) {
    assert(argc > 1);

    char const *filename = argv[1];
    int part = 1;
    if (argc > 2) {
        part = atoi(argv[2]);
    }

    FILE *file = fopen(filename, "r");
    if (file == NULL) {
        fprintf(stderr, "Could not open file \"%s\"\n", filename);
    }
    fseek(file, 0, SEEK_SET);

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

    fclose(file);
}
