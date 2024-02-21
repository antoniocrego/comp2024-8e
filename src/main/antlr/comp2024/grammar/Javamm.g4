grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

EQUALS : '=';
SEMI : ';' ;
LCURLY : '{' ;
RCURLY : '}' ;
LPAREN : '(' ;
RPAREN : ')' ;
NEG : '!' ;
MUL : '*' ;
DIV : '/' ;
ADD : '+' ;
SUB : '-' ;

IMPORT : 'import' ;
CLASS : 'class' ;
EXTENDS : 'extends' ;
INT : 'int' ;
PUBLIC : 'public' ;
RETURN : 'return' ;

IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

INTEGER : [0-9] ;
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
    : name= INT ;

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
    | IF LPAREN expr RPAREN stmt ELSE stmt #IfStmt
    | WHILE LPAREN expr RPAREN stmt #WhileStmt
    | expr SEMI #DefaultStmt
    | expr EQUALS expr SEMI #AssignStmt
    | RETURN expr SEMI #ReturnStmt
    ;

expr
    : NEG expr #Negation
    | expr op=(MUL|DIV) expr #BinaryOp
    | expr op= (ADD|SUB) expr #BinaryOp
    | value=INTEGER #IntegerLiteral
    | name=ID #VarRefExpr
    ;
