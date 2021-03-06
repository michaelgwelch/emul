\	kernel.fs					-- FORTH kernel
\
\	(c) 1996-2009 Edward Swartz
\
\   This program is free software; you can redistribute it and/or modify
\   it under the terms of the GNU General Public License as published by
\   the Free Software Foundation; either version 2 of the License, or
\   (at your option) any later version.
\ 
\   This program is distributed in the hope that it will be useful, but
\   WITHOUT ANY WARRANTY; without even the implied warranty of
\   MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
\   General Public License for more details.
\ 
\   You should have received a copy of the GNU General Public License
\   along with this program; if not, write to the Free Software
\   Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA
\   02111-1307, USA.  
\
\	$Id: kernel.fs,v 1.19 2009-01-11 17:46:42 ejs Exp $

\
\	Core words
\

\	Core words.  If primitives have been defined, these are not used.

\	! # #> #S ' ( * */ */MOD + +! +LOOP , - . ." / /MOD 
\	0< 0= 1+ 1- 2! 2* 2/ 2@ 2drop 2dup 2over 2swap :
\	; < <# = > >body >in >number >r ?dup @ abort abort"
\	abs accept align aligned allot and base begin bl c! c,
\	c@ cell+ cells char char+ chars constant count cr create
\	decimal depth do does> drop dup else emit environment?
\	evaluate execute exit fill find fm/mod here hold i if
\	immediate invert j key leave literal loop lshift m* max min
\	mod move negate or over postpone quit r> r@ recurse
\	repeat rot rshift s" s>d sign source space spaces state
\	swap then type u. u< um* um/mod unloop until variable
\	while word xor [ ['] [char] ] 

\	stack words

[IFUNDEF] 2DROP
: 2DROP		\ core
	drop drop 
;
[THEN]
test" 2drop  2. 3. 2drop 2. d="

[IFUNDEF] 2DUP
: 2DUP		\ core
	over over
;
[THEN]
test" 2dup  1. 2. 2dup 2. d="

[IFUNDEF] 2OVER
: 2OVER	
	2 pick 2 pick
;
[THEN]
test" 2over  1. 2. 2over 1. d= "

[IFUNDEF] 2SWAP
: 2SWAP
	rot >r rot r>
;
[THEN]

[IFUNDEF] >R
\ : >R
\ ;
[THEN]

[IFUNDEF] ?DUP
: ?DUP
	dup if dup then
;
[THEN]

: /cells [ cell<< ] literal rshift ;
: (d) @ - negate /cells ;
[IFUNDEF] DEPTH
: DEPTH
	sp@ sp0 (d)
;
[THEN]

[IFUNDEF] RDEPTH
: RDEPTH
	rp@ rp0 (d)
;
[THEN]

[IFUNDEF] DROP
: DROP
	>r rdrop
;
[THEN]

[IFUNDEF] DUP
: DUP
	>r r@ r>
;
[THEN]

[IFUNDEF] OVER
: OVER
	2>r r@ 2r> swap
;
[THEN]
test" over  1 2 over 1 = swap 2 = and swap 1 = and"

[IFUNDEF] TUCK
: TUCK swap over ;
[THEN]

[IFUNDEF] R>
\ : R>
\ ;
error" need prim r>"
[THEN]

[IFUNDEF] R@
: R@
	r> dup >r
;
[THEN]

[IFUNDEF] SWAP
\ : SWAP
\ ;
error" need prim swap"
[THEN]

\	memory words


[IFUNDEF] ! 
\ : ! 
\ ;
error" need prim !"
[THEN]

[IFUNDEF] +!
: +!
	dup @ over + swap !
;
[THEN]

[IFUNDEF] 2!
: 2!
	2dup ! cell+ !
;
[THEN]

[IFUNDEF] 2@
: 2@
	dup cell+ @ swap @
;
[THEN]

[IFUNDEF] @
\ : @
\ ;
error" need prim @"
[THEN]

[IFUNDEF] C!
\ : C!
\ ;
error" need prim c!"
[THEN]

[IFUNDEF] C@
\ : C@
\ ;
error" need prim c@"
[THEN]

[IFUNDEF] CHAR+
: CHAR+ 
	#char + 
;
[THEN]

[IFUNDEF] CHARS
: CHARS 
	#char * 
;
[THEN]

[IFUNDEF] CELL+
: CELL+
	#cell +
;
[THEN]

[IFUNDEF] CELLS
: CELLS
	#cell *
;
[THEN]


[IFUNDEF] FILL
: FILL
	rot rot 
	over + swap
	?do dup I c! loop drop
;
[THEN]


[IFUNDEF] MOVE
\ note: chars == address units
: MOVE
	>r 2dup u< if r> cmove> else r> cmove then
;
[THEN]


\	math words

[IFUNDEF] *
: *
	um* d>s
;
[THEN]

[IFUNDEF] */
: */
	*/mod swap drop
;
[THEN]

[IFUNDEF] */MOD
: */MOD ( n1 n2 n3 -- n4 n5 ) \ core    star-slash-mod
	>r m* r> sm/rem 
;
[THEN]

[IFUNDEF] +
\ : +
\ ;
error" need prim +"
[THEN]

[IFUNDEF] -
: -
	negate +
;
[THEN]

[IFUNDEF] /
: /
	/mod swap drop
;
[THEN]

[IFUNDEF] /MOD
: /MOD
	>r s>d r> sm/rem
;
[THEN]

[IFUNDEF] 0<
\ : 0<
\ ;
[THEN]

[IFUNDEF] 0=
: 0=
	if 0 else -1 then
;
[THEN]

[IFUNDEF] 1+
: 1+
	1 +
;
[THEN]

[IFUNDEF] 1-
: 1-
	1 -
;
[THEN]

[IFUNDEF] 2*
: 2*
	dup +
;
[THEN]

[IFUNDEF] 2/
: 2/
	1 rshift
;
[THEN]

[IFUNDEF] <
: <
	- 0<
;
[THEN]

[IFUNDEF] =
: =
	- 0=
;
[THEN]

[IFUNDEF] >
: >
	- 0>
;
[THEN]

[IFUNDEF] ABS
: ABS
	dup 0< if negate then
;
[THEN]

[IFUNDEF] AND
\ : AND
\ ;
[THEN]

[IFUNDEF] FM/MOD
: FM/MOD		\ d1 n1 -- n2 n3          core            f_m_slash_mod
\	floored division: d1 = n3*n1+n2, 0<=n2<n1 or n1<n2<=0

	dup >r dup 0< IF  negate >r dnegate r>  THEN
	over       0< IF  tuck + swap  THEN
	um/mod
	r> 0< IF  swap negate swap  THEN
;
[THEN]
test" fm/mod 1ff. f fm/mod 22 = swap 1 = and"
test" fm/mod -1ff. f fm/mod -23 = swap e = and"

[IFUNDEF] INVERT
: INVERT
	negate 1-
;
[THEN]

[IFUNDEF] LSHIFT
: LSHIFT
	0 ?do dup + loop
;
[THEN]

\ [IFUNDEF] M*
: M*
	2dup xor >r 
	abs swap abs 
	um* 
	r> 0< if dnegate then
;
\ [THEN]
test" m* 3 -4 m* -$c. d="
test" m* -4 -3 m* $c. d="

[IFUNDEF] MAX
: MAX
	2dup >= if drop else nip then
;
[THEN]
test" max -5 6 max 6 ="

[IFUNDEF] MIN
: MIN
	2dup <= if drop else nip then
;
[THEN]
test" min 6 -5 min -5 ="

[IFUNDEF] MOD
: MOD
	/mod drop
;
[THEN]

[IFUNDEF] NEGATE
: NEGATE
	invert 1+
;
[THEN]

[IFUNDEF] OR
\ : OR
\ ;
[THEN]

[IFUNDEF] ROT
: ROT	\ a b c -- b c a
	>r swap r> swap
;
[THEN]
test" rot 1 2 3 rot 1 = swap 3 = and swap 2 ="

[IFUNDEF] RSHIFT
: RSHIFT
	0 ?do 2/ loop
;
[THEN]

[IFUNDEF] S>D
\ both endians will have the high word on top
: S>D
	dup 0< if -1 else 0 then
;
[THEN]
test" s>d 45 s>d 0= swap 45 = and"


[IFUNDEF] SM/REM
: SM/REM	( d1 n1 -- n2 n3 )
\ symmetric division: d1 = n3*n1+n2, sign(n2)=sign(d1) or 0
	over >r 
	dup >r abs 
	rot rot
	dabs rot um/mod
	r> r@ xor 0< IF       negate       THEN
	r>        0< IF  swap negate swap  THEN
;
[THEN]
test" sm/rem -$1ff. $f sm/rem -$22 = swap -$1 = and"

[IFUNDEF] U<
\ : U<
\ ;
error" need prim u<"
[THEN]
test" u< 1 2 u<"
test" u< 2 1 u< 0="
test" u< -1 2 u< 0="
test" u< 2 -1 u<"

[IFUNDEF] XOR
\ : XOR
\ ;
error" need prim xor"
[THEN]

[IFUNDEF] TYPE
: TYPE	( caddr n -- )
	0 ?do 
		dup c@ emit 1+
	loop
	drop
;
[THEN]


[IFUNDEF] (.")
: (.")  ( PFA: cstring -- )
\   Warning: this assumes that the IP is stored on the return stack,
\   due to this being a colon definition.
\
    r@ 
    dup c@ $80 >= if 
        dup @ $7fff and >r
        2+
    else
        dup c@ >r
        1+
    then
    r@ type r> r> + 1+ aligned >r
;
[THEN]


[IFUNDEF] ."
: ."
	postpone s"
	state @ if
		[compile] type  -2 here 2- +!        \ !!! TYPE is a deferred word, 
	else
		type
	then
; immediate
[THEN]


| : (skip)
    1 /string
;

[IFUNDEF] >NUMBER
: dn* ( ud un -- ud )
	\			hi.lo 
	\		*		n
	\	-------------
	\	hi.lo*n lo.lo*n
	\	lo.hi*n	0
	dup rot 	\ ( lo-d un un hi-d )
	um* 		\ ( lo-d un d.hiprod )
	drop >r		\ save lo.hi*n
	um* 		\ ( d.loprod )
	0 r> 		\ create d.hiprod
	d+ 
;

\	This ignores '+' and '-' and '.' and stops there.
\   The outer NUMBER interpreter will detect leading signs
\   and perform an initial 
\   and then parse the number 
\   continue parsing, setting 
: >NUMBER	\ CORE ( ud1 c-addr1 u1 -- ud2 c-addr2 u2 )
	begin			\ ( ud1 c-addr1 u1 )
		dup			\ chars left?
		if
			over c@ 	
			base @
			swap digit		\ legal digit?
		else
			0
		then		\ ( ud1 c-addr1 u1 # -1 | ud1 c-addr1 u1 0 )
	while
		>r			\ save digit
		2swap 		\ get accum
		base @ dn*
		r> s>d d+	\ add digit
		2swap
		(skip)		\ advance pointer	
\		2dup . .
	repeat
;
[THEN]

|test : testnum s" 18446744069414584320" ;
\ test" >number 0. testnum  >number 2drop 2dup d. 1. d+ or 0="
|test : testnum s" 4294967295" ;
\ test" >number 0. testnum  >number 2drop 2dup d. 1. d+ or 0="

[IFUNDEF] ABORT"
: (abort")
	rot if cr ." error: " type cr abort else 2drop then
;

: ABORT"
\        Compilation: ( "ccc<quote>" -- )
\
\   Parse ccc delimited by a " (double-quote). Append the run-time semantics
\   given below to the current definition.
\
\        Run-time: ( i*x x1 --  | i*x ) ( R: j*x --  | j*x )
\
\   Remove x1 from the stack. If any bit of x1 is not zero, display ccc and
\   perform an implementation-defined abort sequence that includes the
\   function of ABORT.

	postpone s"
	state @ if
		[compile] (abort")
	else
		(abort")
	then
; immediate
[THEN]

[IFUNDEF] BASE
User BASE
[THEN]

[IFUNDEF] EXECUTE
\ : EXECUTE
\ ;
error" need prim execute"
[THEN]

\ \\\\\\\\\\\\\\\

[IFUNDEF] ]
: ]
	-1 state !
;
[THEN]

[IFUNDEF] I
\ : I
\ ;
[THEN]

[IFUNDEF] J
\ : J
\ ;
[THEN]

[IFUNDEF] LITERAL
: DLITERAL
	state @ if 
		swap postpone lit , postpone lit ,
	then
; immediate

: LITERAL
	state @ if 
		postpone lit ,
	then
; immediate
[THEN]

[IFUNDEF] SLITERAL
: s,	( caddr u -- )
	dup	c, 
	here swap chars dup allot move
;

: SLITERAL	\ C: ( caddr u --  ) R: ( -- caddr u )
	state @ if
		[compile] (s") s, align
	else
		\ copy string to safe place
		>r (spad) @ r@ cmove
		(spad) @ r>
	then
; immediate
[THEN]

[IFUNDEF] S"
: S"
	$22 parse
	postpone sliteral
; immediate
[THEN]

[IFUNDEF] VARIABLE
\ : VARIABLE
\ ;
[THEN]

\	printing words

user hld			\ address of number pad

[IFUNDEF] -pad
: -pad
	(#pad) @
;
[THEN]

[IFUNDEF] <#
: <#	\	 ( -- )
\   Initialize the pictured numeric output conversion process.
 	-pad hld !
;
[THEN]

[IFUNDEF] HOLD
: HOLD	\	( char -- )
\   Add char to the beginning of the pictured numeric output string. An ambiguous condition exists if HOLD executes outside of
\   a <# #> delimited number conversion.
 	-1 hld +! 
	hld @ c!
;

[IFUNDEF] #

[IFUNDEF] M/MOD
: M/MOD	( ud un -- ur udq )
	\ divide high word by base
	>r 0 r@ 	( ud.l ud.h:0 un | R: un ) 
	u/ 			( ud. ud.h*10000%r:ud.h*10000/r | R: un ) 
	r> swap >r	( ud.l:ud.h*10000%r un | R: ud.h*10000/r )
	u/ 			( r q )
	r>			( r q ud.h*10000/r )
;
[THEN]

[IFUNDEF] (#)
: (#)	( ud base -- ud' ch )
	m/mod		\ ( ur udq ) 
	rot 		\ ( udq ur )
	$09 over <	\ ( udq 9<ur )
	if $07 + then 
	$30 + 
;
[THEN]

: #		\   ( ud1 -- ud2 )
\   Divide ud1 by the number in BASE giving the quotient ud2 and the remainder n. (n is the least-significant digit of ud1.)
\   Convert n to external form and add the resulting character to the beginning of the pictured numeric output string. An
\   ambiguous condition exists if # executes outside of a <# #> delimited number conversion.

	base @  
	(#)
	hold
;
[THEN]

[IFUNDEF] #>
: #>	\	( xd -- c-addr u )
\   Drop xd. Make the pictured numeric output string available as a character string. c-addr and u specify the resulting
\   character string. A program may replace characters within the string.
	2drop hld @ -pad over -
;
[THEN]

[IFUNDEF] #S
: #S	\	( ud1 -- ud2 )
\   Convert one digit of ud1 according to the rule for #. Continue conversion until the quotient is zero. ud2 is zero. An
\   ambiguous condition exists if #S executes outside of a <# #> delimited number conversion.
	begin
		#
		2dup or	0=
	until
;
[THEN]
test" abs 394 abs 394 ="
test" abs -395 abs 395 ="

[IFUNDEF] .
: .
	0 .r space
;
[THEN]
test" . -640 . 1"

[IFUNDEF] BL
$20 constant BL
[THEN]

[IFUNDEF] CR
: CR
	$0D emit
;
[THEN]

[IFUNDEF] DECIMAL
: DECIMAL
	$A base !
;
[THEN]

[IFUNDEF] SIGN
: SIGN	\	( n -- )		\ depends on high word being TOS
	0< if
		$2d hold
	then
;
[THEN]

[IFUNDEF] SPACE
: SPACE
	bl emit
;
[THEN]

[IFUNDEF] SPACES
: SPACES
	0 max 0 ?do bl emit loop
;
[THEN]

[IFUNDEF] U.
: U.
	0 u.r space
;
[THEN]


\	string words

[IFUNDEF] COUNT
: COUNT
	dup c@ swap 1+ swap
;
[THEN]

[IFUNDEF] WORD
: (parse-word)	( ch -- caddr u )
	(skip-spaces)
	parse				\ get new word
	\ 2dup type
;

: WORD	\	( char "<chars>ccc<char>" -- c-addr )
	(parse-word) 
	2dup + bl swap c!	\ word ends with space
	-pad (>c) 			\ copy to word pad
	-pad				\ leave addr
;
[THEN]


\	I/O words

[IFUNDEF] ACCEPT

| : overstrike
	8 emit bl emit 8 emit
;

: ACCEPT
\        ( c-addr +n1 -- +n2 )
\   Receive a string of at most +n1 characters. An ambiguous condition exists if +n1 is zero or greater than 32,767. Display
\   graphic characters as they are received. A program that depends on the presence or absence of non-graphic characters in
\   the string has an environmental dependency. The editing functions, if any, that the system performs in order to construct
\   the string are implementation-defined.
\
\	(EJS: this one does not automatically abort at n1 chars.)
	swap >r		\ store c-addr on R:
	0			\ position
	begin
		key
		dup $0d <>
	while
		dup 8 <> over &211 ( fctn-s ) <> and
		if
			>r			\ store key
			\ go back if too many chars
			2dup <= if 1- 8 emit then
			\ show key
			r@ emit		
			\ store code
			dup r> swap r@ + c!	
			1+
		else
		    \ don't go too far
			drop dup 0 > if		
				overstrike	\ backspace
				1-
			then
		then
	repeat
	drop		\ key
	min			\ lose max #chars
	rdrop
;

\ : prompt ." type stuff> " ;
\ test" accept prompt pad 5 accept pad swap $2a emit type $2a emit  1"
[THEN]

[IFUNDEF] EMIT
\ : EMIT
\ 	drop
\ ;
error" need emit"
[THEN]

[IFUNDEF] ENVIRONMENT?
: ENVIRONMENT?
	2drop 0
;
[THEN]

[IFUNDEF] KEY
\ : KEY
\ ;
error" need key"
[THEN]

: (quit?)
	dup &81 = swap &113 = or
;

: (pause?)	( -- <t|f to quit> )
	key? dup if            
	    ( t ) 
		key (quit?) 0= if		\ not quit
		    \ pause and wait for something else 
			drop key (quit?)
	    else
	        ( t )
		then
    \ else false, no key, so no quit		
	then
;

\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

\	CORE EXT words
\	#tib .( .r 0<> 0> 2>r 2r> 2r@
\	:noname <> ?do again c" case compile,
\	convert endcase endof erase expect
\	false hex marker nip of pad parse
\	pick query refill restore-input roll
\	save-input source-id span tib to
\	true tuck u.r u> unused value
\	within [compile] \ 

[IFUNDEF] #TIB
\ current size of TIB
User #TIB
[THEN]

[IFUNDEF] .(
\ : .(
\ ;
[THEN]

[IFUNDEF] .R
\ : .R
\ ;
[THEN]

[IFUNDEF] 0<>
\ : 0<>
\ ;
[THEN]

[IFUNDEF] 0>
\ : 0>
\ ;
[THEN]

[IFUNDEF] 2>R
\ : 2>R
\ ;
[THEN]

[IFUNDEF] 2R>
\ : 2R>
\ ;
[THEN]

[IFUNDEF] 2R@
\ : 2R@
\ ;
[THEN]

[IFUNDEF] :NONAME
\ : :NONAME
\ ;
[THEN]

[IFUNDEF] <>
: <>	\ core ext
	= NOT
;
[THEN]

[IFUNDEF] .R
: .R
	>r				\ field width
	s>d				\ make double
	dup >r			\ sign
	dabs <# #S r> sign #>
	r> over - spaces
	type
;
[THEN]


[IFUNDEF] C"
\ : C"
\ ;
[THEN]

[IFUNDEF] CONVERT
\ : CONVERT
\ ;
[THEN]

[IFUNDEF] ERASE
\ : ERASE
\ ;
[THEN]

[IFUNDEF] EXPECT
\ : EXPECT
\ ;
[THEN]

[IFUNDEF] FALSE
0 constant FALSE
[THEN]

[IFUNDEF] HEX
: HEX
	$10 base !
;
[THEN]

[IFUNDEF] MARKER
\ : MARKER
\ ;
[THEN]

[IFUNDEF] NIP
\ : NIP
\ ;
[THEN]

[IFUNDEF] PAD
: PAD
	(pad) @
;
[THEN]

[IFUNDEF] (match)		

\	Match 'char' inside [caddr..caddr+u) and return length of word
: (match)		( caddr u char -- u )

	over >r			\ save original #chars
	>r				\ store char 
	begin
		dup			\ any more chars left?
		if
			over c@  	\ ( caddr u ch' )
			r@ 			\ ( caddr u ch' ch )
			<>			\ ( caddr u t/f )
		else
			0
		then
	while
		1- swap 1+ swap
	repeat
	rdrop

	swap drop		( u' )
	r> 				( u' u )
	swap -			( len )
;
[THEN]

\ : mystr s" 1111123456" ;
\ test" (match) mystr $32 (match) mystr drop swap type 1"

\	Return bounds of remaining source
: (src>)		( -- caddr u )
	source 	
	>in @ 
	- 0 max
	swap >in @ + swap
;

\	(>src) advances >in by u' bytes
: (>src)			\ ( u' -- )
	1+ >in +!		\ update >in
;


: PARSE	 	( char "ccc<char>" -- c-addr u )
\   Parse ccc delimited by the delimiter char.
\   c-addr is the address (within the input buffer) and u is the length of the parsed string. If the parse area was empty, the
\   resulting string has a zero length.
	(src>) over >r 
	rot (match)
	dup (>src)
	r> swap
;

[ENDIF]

\	Skip spaces in source
| : (skip-spaces)	( -- )
	(src>)
	0 ?do
		dup c@ bl > if
			leave
		else
			1+ 1 >in +!
		then
	loop
	drop
;


[IFUNDEF] PICK
\ : PICK
\ ;
[THEN]

[IFUNDEF] ROLL
\ : ROLL
\ ;
[THEN]

[IFUNDEF] SPAN
\ : SPAN
\ ;
[THEN]

[IFUNDEF] TO
\ : TO
\ ;
[THEN]

[IFUNDEF] TRUE
-1 constant TRUE
[THEN]

[IFUNDEF] TUCK
: TUCK
	dup >r swap r>
;
[THEN]

[IFUNDEF] U.R
: U.R
	>r 
	0			\ make double
	<# #S #>
	r> over - spaces
	type
;
[THEN]


[IFUNDEF] U>
\ : U>
\ ;
[THEN]

[IFUNDEF] UNUSED
\ : UNUSED
\ ;
[THEN]

[IFUNDEF] VALUE
\ : VALUE
\ ;
[THEN]

[IFUNDEF] WITHIN
: WITHIN	( test low high -- flag )   
	over - >r - r> u<
;
[THEN]

[IFUNDEF] \
: \
	blk @
	if 
		\ block file
		>in @ c/l / 1+ c/l * >in ! exit
	else
		\ consume rest of buffer
		source >in ! drop 
	then
; immediate
[THEN]

\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


\
\	STRING words
\

[IFUNDEF] CMOVE
\ : CMOVE
\ ;
[THEN]

[IFUNDEF] CMOVE>
\ : CMOVE>
\ ;
[THEN]

[IFUNDEF] /STRING
\ : /STRING
\ ;
[THEN]

[IFUNDEF] COMPARE
\ : COMPARE
\ ;
[THEN]


\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\


\
\	Double words
\

[IFUNDEF] D.	
: D.R
	>r				\ field width
	dup >r			\ sign
	dabs <# #S r> sign #>
	r> over - spaces
	type
;
: D.		\ DOUBLE
	0 d.r space
;
[THEN]

[IFUNDEF] UD.
: UD.R	\ double
	>r
	<# #S #>
	r> over - spaces
	type
;
: UD.	\ double
	0 UD.R
	space
;
[THEN]

[IFUNDEF] D-
: D-
	DNEGATE D+
;
[THEN]

[IFUNDEF] D<
: D<
	D- D0<
;
[THEN]
test" d< 20. 40. d<"
test" d< 50. 40. d< 0="
test" d< -20. -10. d<"
test" d< -10. -20. d< 0="
test" d< -10. 10. d<"
test" d< 10. -10. d< 0="


\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

\
\	TOOLS words
\

[IFUNDEF] .S
: .S
	." <" depth dup 0 u.r [char] : emit abs $10 min 0 ?do
		depth i - 1- pick u.
	loop ." >"
;
[THEN]

[IFUNDEF] r.S
: r.S
	rdepth >r
	." <" r@ 0 u.r [char] : emit 
	r@ $20 min 0 ?do
		j i - rpick u.
	loop ." >" 
	rdrop
;
[THEN]

[IFUNDEF] ?
: ? @ . ;
[THEN]

\ \\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\\

