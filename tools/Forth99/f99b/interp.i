

[IFUNDEF] (
: (
    $29 parse 2drop
;   immediate target-only
[THEN]

[IFUNDEF] >IN
user >IN
[THEN]

[IFUNDEF] CHAR
: CHAR  ( -- c )
    bl (parse-word)
    drop c@
;
[THEN]

[IFUNDEF] REFILL
: REFILL    ( -- flag )

\   Attempt to fill the input buffer from the input source, returning
\   a true flag if successful.
\
\   When the input source is the user input device, attempt to receive
\   input into the terminal input buffer. If successful, make the
\   result the input buffer, set >IN to zero, and return true. Receipt
\   of a line containing no characters is considered successful. If
\   there is no input available from the current input source, return
\   false.
\
\   When the input source is a string from EVALUATE, return false and
\   perform no other action.
\
\   When the input source is a block, make the next block the input source
\   and current input buffer by adding one to the value of BLK and setting
\   >IN to zero. Return true if the new value of BLK is a valid block number,
\    otherwise false.
 
    blk @ ?dup if
               1 blk +!
               block >tib !  chars/block #tib !
               0 >in !
               true
    
    else
        source-id
        dup -1 = if         \ evaluate string
            drop false      \ end.
        else ?dup 0= if         \ user input device
            (tib0) $100 accept #tib !
            (tib0) >tib !
            0 >in ! 
            bl emit
            true
        else 
            ( fileid )
\ TODO            
\            (tib0) $100 rot (rdln) 0=
\            if
\    
\    \  ." [" (tib0) over type ." ]" cr
\    
\                #tib !
\                (tib0) >tib !
\                0 >in !
\                true
\            else
\    \           ." end of file" cr
\                \ drop false
\            then
        then then
    then
;
[THEN]

[IFUNDEF] QUERY
\ : QUERY
\ ;
[THEN]

[IFUNDEF] RESTORE-INPUT
: RESTORE-INPUT ( xn ... x1 n -- )
    6 <> abort" invalid restore-input"
    >in !
    blk !
    #tib ! >tib !
    loadline ! loadfile !
;
[THEN]

[IFUNDEF] SAVE-INPUT
: SAVE-INPUT    ( -- xn ... x1 n )
    loadfile @ loadline @
    >tib @ #tib @
    blk @
    >in @
    6
;
[THEN]

[IFUNDEF] SOURCE-ID
: SOURCE-ID
    loadline @ 0< if
        -1
    else
        loadfile @
    then
;
[THEN]

[IFUNDEF] TIB
User >tib
: TIB
    >tib @
;
[THEN]


[IFUNDEF] INTERPRET

\   INTERPRETER
\   ===========

\   Source state is represented as follows:
\   If blk=0, 'source-id' is 0 for keyboard, -1 for evaluate string, >0 for text file.
\   else blk=blk # for source.
\   For non-block stuff, we use loadfile/loadline to keep track of file source,
\   and >tib/#tib for all input strings.
\   loadfile=0 for user input, loadline<0 for evaluate string.

User loadfile
User loadline

\   Push the input state.
: <input        ( xn ... x1 n -- R: xn ... x1 n )
    r>
    loadfile @ >r loadline @ >r
    >tib @ >r   #tib @ >r
    blk @ >r    
    >in @ >r
    >r  
;

: input>
    r>
    r> >in !
    r> blk !
    r> #tib !   r> >tib !
    r> loadline ! r> loadfile !
    >r
;

: (>c)      ( caddr u naddr -- )
\   make counted string at naddr
    2dup c!             \ set length byte
    1+ swap cmove>      \ move data
;

: ,"
    postpone s" dup >r  here (>c)  r> 1+ aligned allot  
;



: (find)      ( c-addr lfa -- c-addr 0 | nfa 1 ) 
    \ find word in dictionary    ( c-addr lfa -- c-addr 0 | nfa 1 ) 
    \ lfa is nfa-2
    
    \ dbg

    swap >r
    
    begin
        dup
    while
        r@ over  2+             \ nfa
        nfa=
        if
            rdrop
            2+
            1
            exit
        else
            @            
        then
    repeat
    drop r> 0
    
    \ dbgf
;
    
: nfa=  ( caddr nfa -- 1 | 0 )
    dup c@  $80 and if   \ not hidden?
        2>r
        r@  1+  r>  c@ $3f and  \ nfa --> caddr n
        r@  1+  r>  c@          \ caddr -> caddr n
        comparef  0=
    else
        2drop 0
    then
    
;
    


: (lookup)      ( c-addr u -- caddr 0 | nfa 1|-1 )
    here (>c)   \ make counted string + NFA
\ context @ @ (find) dup 0= if ... 

[ 1 [if] ]
    here latest
    (find) 
[ [else] ]
    here find dup >r if xt>nfa else drop then r>
    
[ [then] ]
;

: ?stack
    depth 0< if
        ." stack empty!" cr
        abort
    then
;

: huh?  ( caddr -- )
    count type space
    [char] ? emit cr
;

[IFUNDEF] NUMBER

\   Interpret counted string as number,
\   and store decimal point location in DPL.

User dpl

: number    ( addr -- ud )
\   .s

    0 0 rot
    
    base @ >r               \ save original base

    count 

    \ see if first char is '-'
    over c@ [char] - = dup 
    >r                      \ store sign flag
    if (skip) then

    \ check for base conversion
    over c@ [char] $ = if
        hex (skip)          \ use hex for '$'
    else over c@ [char] & = if
        decimal (skip)      \ use decimal for '&'
    then then

    -1 dpl !

    >number
    dup if      \ any invalid chars?
        drop over c@ [char] . = if    \ did we stop at '.'?
            over dpl !      \ don't store offset... too much work ;)
            (skip)          \ skip '.'
            >number         \ parse rest
        then
        dup if 
            here huh? 2drop 2drop quit  \ error
        then
    then
    2drop

    r>              \ sign flag
    if dnegate then

    r> base !       \ original base


\   .s
;

[ENDIF]

| : interpreter
\        ( i*x c-addr u -- j*x )
\
\   Interpret one word
    (lookup)        \ ( caddr 0 | nfa 1 )
    if
        dup nfa>imm? 0=
        state @ and     \ compiling and not immediate?
        if
            nfa>xt compile,
        else
            nfa>xt execute
        then
    else
        number dpl @ 1+ if
            state @ if  postpone dliteral  then 
        else 
            drop state @ if  postpone literal  then
        then 
    then
;

: interpret
    begin
        ?stack
        bl (parse-word) 
        dup
    while
        interpreter
    repeat
    2drop
;

: EVALUATE  ( i*x c-addr u -- j*x )
    <input
    -1 loadline !
    0 loadfile !
    0 blk !
    #tib !  >tib !  
    0 >in !
    interpret
    input>
;
[THEN]

[IFUNDEF] QUIT

: (clrsrc)
    0 blk ! 0 loadfile ! 0 loadline !
;

: QUIT
    begin
        sp0 @ sp!
        rp0 @ rp! 
        (clrsrc)
        postpone [  
        .s cr
        begin   
            ints-check
            refill
        while
            interpret

            \ print comments only when using user input
            blk @ source-id and 0= if 
                state @ 0= if
                    ."  ok" .s
                then
                cr
            then
        repeat
    again
;
[THEN]

[IFUNDEF] SOURCE
: SOURCE    ( -- caddr u )
    blk @ ?dup if
        block chars/block
    else
        >tib @ #tib @
    then
;
[THEN]

[IFUNDEF] [CHAR]
: [CHAR]
    char postpone literal ; immediate  target-only
[THEN]

[IFUNDEF] [IF]   ( i.e., should we define the directives? )

\ User parsed
\ [[[ $20 tudp @ + tudp !  $20 tup @ + tup ! ]]]
\ 
: str= compare 0= ;
\ : toupper dup $61 $7b within if $20 - then ;
\ 
\ : upcase ( str -- )
\ count bounds
\     ?DO I c@ toupper I c! LOOP ;
\ : place  ( addr len to -- ) \ gforth
\     over >r  rot over 1+  r> move c! ;
\ : bounds ( addr u -- addr+u addr ) 
\     over + swap ;
: comment? ( c-addr u -- c-addr u )
        2dup s" (" str=
        IF    postpone (
        ELSE  2dup s" \" str= IF postpone \ THEN
        THEN ;

: [ELSE]
    1 BEGIN
    BEGIN bl word count dup WHILE
        comment? 
        2dup s" [IF]" str= >r 
        2dup s" [IFUNDEF]" str= >r
        2dup s" [IFDEF]" str= r> or r> or
        IF   2drop 1+
        ELSE 2dup s" [ELSE]" str=
        IF   2drop 1- dup
            IF 1+
            THEN
        ELSE
            2dup s" [ENDIF]" str= >r
            s" [THEN]" str= r> or
            IF 1- THEN
        THEN
        THEN
        ?dup 0= if exit then
    REPEAT
    2drop refill 0=
    UNTIL drop 
; immediate  target-only 
  
: [THEN] ( -- ) ; immediate  target-only

: [ENDIF] ( -- ) ; immediate  target-only

: [IF] ( flag -- )
    0= IF postpone [ELSE] THEN ; immediate  target-only 

: defined? bl word find nip ;
: [IFUNDEF] defined? 0= postpone [IF] ; immediate  target-only
: [IFDEF] defined? postpone [IF] ; immediate  target-only

[THEN]      ( done defining the directives )

