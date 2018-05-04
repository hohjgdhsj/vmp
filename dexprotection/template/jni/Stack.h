/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

/*
 * Stack frames, and uses thereof.
 */
#ifndef DALVIK_INTERP_STACK_H_
#define DALVIK_INTERP_STACK_H_

#include "jni.h"
#include <stdarg.h>
#include "YcFile.h"
/*
Stack layout

In what follows, the "top" of the stack is at a low position in memory,
and the "bottom" of the stack is in a high position (put more simply,
they grow downward).  They may be merged with the native stack at a
later date.  The interpreter assumes that they have a fixed size,
determined when the thread is created.

Dalvik's registers (of which there can be up to 64K) map to the "ins"
(method arguments) and "locals" (local variables).  The "outs" (arguments
to called methods) are specified by the "invoke" operand.  The return
value, which is passed through the interpreter rather than on the stack,
is retrieved with a "move-result" instruction.

    Low addresses (0x00000000)

                     +- - - - - - - - -+
                     -  out0           -
                     +-----------------+  <-- stack ptr (top of stack)
                     +  VM-specific    +
                     +  internal goop  +
                     +-----------------+  <-- curFrame: FP for cur function
                     +  v0 == local0   +
+-----------------+  +-----------------+
+  out0           +  +  v1 == in0      +
+-----------------+  +-----------------+
+  out1           +  +  v2 == in1      +
+-----------------+  +-----------------+
+  VM-specific    +
+  internal goop  +
+-----------------+  <-- frame ptr (FP) for previous function
+  v0 == local0   +
+-----------------+
+  v1 == local1   +
+-----------------+
+  v2 == in0      +
+-----------------+
+  v3 == in1      +
+-----------------+
+  v4 == in2      +
+-----------------+
-                 -
-                 -
-                 -
+-----------------+  <-- interpStackStart

    High addresses (0xffffffff)

Note the "ins" and "outs" overlap -- values pushed into the "outs" area
become the parameters to the called method.  The VM guarantees that there
will be enough room for all possible "outs" on the stack before calling
into a method.

All "V registers" are 32 bits, and all stack entries are 32-bit aligned.
Registers are accessed as a positive offset from the frame pointer,
e.g. register v2 is fp[2].  64-bit quantities are stored in two adjacent
registers, addressed by the lower-numbered register, and are in host order.
64-bit quantities do not need to start in an even-numbered register.

We push two stack frames on when calling an interpreted or native method
directly from the VM (e.g. invoking <clinit> or via reflection "invoke()").
The first is a "break" frame, which allows us to tell when a call return or
exception unroll has reached the VM call site.  Without the break frame the
stack might look like an uninterrupted series of interpreted method calls.
The second frame is for the method itself.

The "break" frame is used as an alternative to adding additional fields
to the StackSaveArea struct itself.  They are recognized by having a
NULL method pointer.

When calling a native method from interpreted code, the stack setup is
essentially identical to calling an interpreted method.  Because it's a
native method, though, there are never any "locals" or "outs".

For native calls into JNI, we want to store a table of local references
on the stack.  The GC needs to scan them while the native code is running,
and we want to trivially discard them when the method returns.  See JNI.c
for a discussion of how this is managed.  In particular note that it is
possible to push additional call frames on without calling a method.
*/


struct StackSaveArea;

//#define PAD_SAVE_AREA       /* help debug stack trampling */

/*
 * The VM-specific internal goop.
 *
 * The idea is to mimic a typical native stack frame, with copies of the
 * saved PC and FP.  At some point we'd like to have interpreted and
 * native code share the same stack, though this makes portability harder.
 */
struct StackSaveArea {

    /* saved frame pointer for previous frame, or NULL if this is at bottom */
    u4*         prevFrame;

    /* saved program counter (from method in caller's frame) */
    const u2*   savedPc;

    /* pointer to method we're *currently* executing; handy for exceptions */
    const SeparatorData* method;

    union {
        /* for JNI native methods: bottom of local reference segment */
        u4          localRefCookie;

        /* for interpreted methods: saved current PC, for exception stack
         * traces and debugger traces */
        const u2*   currentPc;
    } xtra;
};

/* move between the stack save area and the frame pointer */
#define SAVEAREA_FROM_FP(_fp)   ((StackSaveArea*)(_fp) -1)
#define FP_FROM_SAVEAREA(_save) ((u4*) ((StackSaveArea*)(_save) +1))

/* when calling a function, get a pointer to outs[0] */
#define OUTS_FROM_FP(_fp, _argCount) \
    ((u4*) ((u1*)SAVEAREA_FROM_FP(_fp) - sizeof(u4) * (_argCount)))

/* reserve this many bytes for handling StackOverflowError */
#define STACK_OVERFLOW_RESERVE  768

/*
 * Determine if the frame pointer points to a "break frame".
 */
inline bool dvmIsBreakFrame(const u4* fp)
{
    return SAVEAREA_FROM_FP(fp)->method == NULL;
}
#endif  // DALVIK_INTERP_STACK_H_
