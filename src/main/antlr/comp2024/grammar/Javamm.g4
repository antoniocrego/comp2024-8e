grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
COMMA : ',' ;
PERIOD : '.' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
LSPAREN : '[' ;
RSPAREN : ']' ;
NEG : '!' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

IMPORT : 'import' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;
NEW : 'new' ;

LENGTH : 'length' ;

INTEGER : [0-9]+ ;
ID : [a-zA-Z]+ ;

WS : [ \t\n\r\f]+ -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT ID('.'ID)*';'
    ;

classDecl
    : CLASS name=ID
        (EXTENDS ID )?
        LCURLY
        varDecl*
        methodDecl*
        RCURLY
    ;

varDecl
    : type name=ID SEMI
    ;

type
    : name= INT
    | name= INT LSPAREN RSPAREN
    | name= BOOLEAN
    | name= ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;})?
        type name=ID
        LPAREN param RPAREN
        LCURLY varDecl* stmt* RCURLY
    ;

param
    : type name=ID
    ;

stmt
    : expr EQUALS expr SEMI #AssignStmt //
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : NEG expr #Negation
    | expr op=(MUL|DIV) expr #BinaryExpr //
    | expr op= (ADD|SUB) expr #BinaryExpr //
    | expr '.' LENGTH # LengthExpr
    | expr '[' index=expr ']' #ArrayAccess //
    | NEW INT '[' index=expr ']' # NewArray //
    | NEW ID '[' ']' # NewClass //
    | '[' (expr (',' expr)*)? ']' # ArrayInit //
    | value=INTEGER #IntegerLiteral //
    | name=ID #VarRefExpr //
    ;
