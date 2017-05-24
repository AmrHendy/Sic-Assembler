Prbn01   START   1000
         LDA     =C'TEST'
         LDA     BETA
         MUL     GAMMA
         STA     ALPHA
         LTORG
ALPHA    EQU     1
BETA     EQU     ALPHA
DELTA    EQUA    BETA - ALPHA
GAMMA    WORD    4
DATA     EQU     *
         LDA     =X'45'
         END     Prbn01
