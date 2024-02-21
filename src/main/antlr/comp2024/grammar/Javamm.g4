grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
ELLIPSIS : '...';
SEMI : ';' ;
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
LT : '<' ;
AND : '&&' ;

IMPORT : 'import' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
INT : 'int' ;
BOOLEAN : 'boolean' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

INTEGER : [0-9]+ ;
ID : [_$a-zA-Z][_$a-zA-Z0-9]* ;

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
    : name=INT
    | name=INT '[' ']'
    | name=INT '...'
    | name=BOOLEAN
    | name=ID
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
    : LCURLY stmt* RCURLY #StmtBody
    | IF '(' expr ')' stmt ELSE stmt #IfStmt
    | WHILE '(' expr ')' stmt #WhileStmt
    | expr ';' #DefaultStmt
    | expr EQUALS expr ';' #AssignStmt
    | RETURN expr ';' #ReturnStmt
    ;

expr
    : NEG expr #Negation
    | expr op=(MUL|DIV) expr #BinaryOp
    | expr op=(ADD|SUB) expr #BinaryOp
    | expr op=LT expr #BinaryOp
    | expr op=AND expr #BinaryOp
    | value=INTEGER #IntegerLiteral
    | name=ID #VarRefExpr
    ;
