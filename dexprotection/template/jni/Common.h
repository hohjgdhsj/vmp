#pragma once

#include <stdint.h>

typedef uint8_t             u1;
typedef uint16_t            u2;
typedef uint32_t            u4;
typedef uint64_t            u8;
typedef int8_t              s1;
typedef int16_t             s2;
typedef int32_t             s4;
typedef int64_t             s8;

/**
 * 计算数组元素个数。只能用于数组，绝不能用于指针。
 */
#define array_size(arr) (sizeof(arr)/sizeof((arr)[0]))

union JValue {

    u1      z;
    s1      b;
    u2      c;
    s2      s;
    s4      i;
    s8      j;
    float   f;
    double  d;
    int* l;

};
