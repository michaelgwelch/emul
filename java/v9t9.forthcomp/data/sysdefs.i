
\   ---------   memory map

$0000       constant    I/O

$0000       constant    VDPRD
$0002       constant    VDPST
$0008       constant    VDPWD
$000a       constant    VDPWA
$000c       constant    VDPCL
$000e       constant    VDPWI

$0010       constant    GPLRD
$0012       constant    GPLRA
$0014       constant    GPLWD
$0016       constant    GPLWA
$0018       constant    SPCHRD
$001a       constant    SPCHWT

$0020       constant    KBD

$0040       constant    SOUND  \ ... 0x20!

\   ---------  peripherals

$0080       constant    'INTS
    
    $0      constant    'INT_BKPT
    $1      constant    'M_INT_BKPT
    $1      constant    'INT_EXT
    $2      constant    'M_INT_EXT
    $2      constant    'INT_VDP
    $4      constant    'M_INT_VDP

$0082       constant    'KBD
$0083       constant    'KBDA

\   -----------------------    

$0400       constant    ROM



$ffc0       constant    SysCalls    \ ... 0x20

$ffe0       constant    IntVecs     \ ... 0x20

    15      constant    INT_RESET
    14      constant    INT_NMI
    1       constant    INT_VDP
    0       constant    INT_BKPT

\ -----------   GROM addresses

$0          constant    grom_kbdlist
$130        constant    grom_font8x8
$930        constant    grom_font5x6

\ -----------   constants

7           constant    CTX_INT        

