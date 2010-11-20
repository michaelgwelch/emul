
create  VRegSave      16 allot

:   w>b ( w -- lo hi )
    dup $ff and swap 8 urshift  
;

:   write-vregaddr ( reg -- )
    dup  
    vwaddr
    $7fff and w>b   dup 16 <  if  VRegSave + c!  else drop then
;

:   get-vreg ( reg# -- val )
    VRegSave + c@
;

\   (Not a good reason for locals, since they're much slower, but just have something for now)

::   write-vregs ( reglist -- )
    begin 
        reglist @ dup
    while
        write-vregaddr
        2 'reglist +!
    repeat   
    drop
;

: vid-show ( f -- )
    $40 and 
    1 get-vreg  or
    $8100 or  write-vregaddr
;

\ ---------------------

56      RamVar  v-mode

: +field    ( "name" ptr -- ptr' )    dup Constant 2+ ; immediate

v-mode
    +field  v-screen       \ VDP addr of screen
    +field  v-screensz     \ VDP size of screen
    +field  v-patts        \ VDP addr of patterns
    +field  v-pattsz       \ VDP size of pattern table
    +field  v-colors       \ VDP addr of colors
    +field  v-colorsz      \ VDP size of color table
    +field  v-sprites      \ VDP addr of sprites
    +field  v-sprcol       \ VDP addr of sprite color table (0 if not sprite 2 mode)
    +field  v-sprpat       \ VDP addr of sprite patterns
    +field  v-sprmot       \ VDP addr of sprite motion
    +field  v-free         \ usable space
    +field  v-width        \ chars across
    +field  v-height       \ chars down
    
    +field  v-coordaddr    ( x y -- addr bit )
    +field  v-drawchar     ( ch addr bit -- )
    +field  v-savechar     ( addr bit buff -- )
    +field  v-restorechar  ( buff addr bit -- )
    +field  v-drawcursor   ( addr bit -- )
    +field  v-setupmode    ( -- )
    +field  v-updatecolors ( byte -- )
    +field  v-setfont      ( addr -- )
drop

2   RamVar v-font                           \ GROM addr of current font
 
8   RamVar v-curs-under  

1   RamVar v-curs                         \ state of cursor (0=off, 1=on)
1   RamVar v-cursor-timer                 \ current iter

20 Constant v-cursor-blink              \ 30/60 sec
 
:   write-var-list   ( table -- )
    begin
        dup @ 
    while
        dup  d@  swap !
        4 +
    repeat
    drop
;

: (mode)
    dup  0 last-video-mode within  0= -&24 ?error
    cells  video-modes +  @ execute
;

1   RamVar fg
1   RamVar bg

: color-byte
    fg c@ 4 lshift  bg c@  or
;

: v-refresh-colors
    color-byte v-updatecolors @  execute
;

1 <export
: fg!
    fg c!
    v-refresh-colors
;

: bg!
    bg c!
    v-refresh-colors
;

: fg@
    fg c@
;   

: bg@
    bg c@
;

export>

\   Reset video state
\
\   -- reset terminal bounds
\   -- clear memory
\   -- load font (if needed)

: vreset
    term-reset
    cls    

    v-setupmode @  execute
    v-refresh-colors
    
    v-font @  v-setfont @  execute
    
    true vid-show
;

: vstdpal

;



:   v-cursor-on ( addr bit -- )
    v-curs c@ not if
        2dup
        v-curs-under v-savechar @  execute
        
        v-drawcursor @ execute
        true v-curs c!
    else
        2drop
    then
;
:   v-cursor-off  ( addr bit -- )
    v-curs c@ if
        2>r v-curs-under 2r>  v-restorechar @  execute
        false v-curs c!
    else
        2drop
    then
;

:   update-cursor
    1 v-cursor-timer c+!
    v-cursor-timer c@  v-cursor-blink >= if
        0 v-cursor-timer c!
        curs-addr
        v-curs c@ if 
            v-cursor-off
        else
            v-cursor-on
        then
    then
;


\ ---------------------

Create video-modes
    ' text-mode ,
    ' gfx-mode ,
    0 ,
    0 ,
    0 ,
    0 ,
    0 ,
    0 ,
    ' text2-mode ,
    0 ,
    0 ,
    
1 <export
$000    constant    text
$001    constant    gfx
$002    constant    bitmap      $002    constant    gfx2
$003    constant    gfx3           
$004    constant    gfx4
$005    constant    gfx5
$006    constant    gfx6
$007    constant    gfx7
$008    constant    text2
$009    constant    mono
$00A    constant    multi
export>

$00B    constant    last-video-mode

1 <export
\ tables for (vtab)
&0 constant #scr
&1 constant #ssz
&2 constant #col
&3 constant #csz
&4 constant #pat
&5 constant #psz
&6 constant #spt
&7 constant #spr
&8 constant #smt
&9 constant #spc
&10 constant #fre
export>

include video_base.i


\ ---------------------

1 <EXPORT

:   mode ( num -- )
    (mode) vreset vstdpal
;


:   vfill ( ch addr len -- )
    swap $4000 or vwaddr
    VDPWD swap 0 (cfill) 
;

:   v-clear ( ch -- )
    v-screen @ v-screensz @ vfill
;

:   (setfont)
    v-font @  v-setfont @  execute
;

:   font8x8
    grom_font8x8 v-font !
    (setfont)
;

EXPORT>


:   video-init
    \ reset latches
    VDPRD c@ drop
    0 GPLWD c! 
    
    text mode

    $1 fg!
    $7 bg!
    
    font8x8    
    
;


