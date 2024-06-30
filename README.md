# Polykill Deobfuscator
A simple deobfuscator written in Java for the Polykill js file, which originally was at https://www.googie-anaiytics.com/ga.js.
The version used here is at https://pastebin.com/raw/hAgtHd70.
The deobfuscator depends on [Rhino](https://github.com/mozilla/rhino) as a Javascript parser.
It uses several steps to deobfuscate the code:
1. Evaluating constant expressions (e.g. `(-0x17*0x12d+-0x1*-0x1b13+-0x1*0x7)` into `1`).
2. Replacing references into objects with their constant value if possible (e.g. `a0_0x5dea1e._0x3b6b41` is converted into `0x150` because `_0x3b6b41` only gets assigned once in the object definition of `a0_0x5dea1e`).
3. Removing those table entries if they are no longer in use.
4. Removing those tables entirely when they are no longer in use.
5. Inlining functions that immediately return (e.g. `function _0x168ef0(_0x5ebce6,_0x4b3414,_0x26a284,_0x438748,_0x439328){return a0_0xa0b8(_0x5ebce6- -a0_0x55194f._0x48f80d,_0x4b3414);}`).
6. Internally running the decoding function `a0_0xa0b8`.
7. Removing `if (false) ... else ...` / `if (true) ... else ...` / `true ? ... : ...` / `false ? ... : ...`.
8. Removing nesting of if statements within the else clause and replacing them with else if.
9. Replacing array indexing using strings with property gets (e.g. `_0x208c16['push']` is replaced with `_0x208c16.push`).
10. Splitting variable declarations (e.g. `const a0_0x5dea1e={...},a0_0x5de57c={...},a0_0x55194f={...};`  is replaced with `const a0_0x5dea1e={...}; const a0_0x5de57c={...}; const a0_0x55194f={...};`).

Some things to note:
1. The program is poorly written and might have bugs when used on similar programs - often some cases are not handled.
2. This version of the deobfuscator does not include the decoding function `a0_0xa0b8` - if you want it, remove the line that says `usedFunctions.remove("a0_0xa0b8");`.
3. The deobfuscator takes its input from the clipboard.
4. The deobfuscator uses its own function to convert the AST to a string because Rhino's toSource creates broken indentation.
