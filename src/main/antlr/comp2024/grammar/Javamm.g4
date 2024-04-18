grammar Javamm;

@header {
    package pt.up.fe.comp2024;
}

// Method access specifiers
ACCESS_TYPE: ( FINAL | STATIC | ABSTRACT | TRANSIENT | SYNCHRONIZED | VOLATILE ) ;
FINAL: 'final' ;
STATIC: 'static' ;
ABSTRACT: 'abstract';
TRANSIENT: 'transient' ;
SYNCHRONIZED: 'synchronized' ;
VOLATILE: 'volatile' ;

IMPORT : 'import' ;
EXTENDS : 'extends' ;
BOOLEAN : 'boolean' ;

CLASS : 'class' ;

PUBLIC : 'public' ;
PRIVATE :  'private';
PROTECTED : 'protected';

IF : 'if' ;
ELSE : 'else' ;
WHILE : 'while' ;

TRUE : 'true';
FALSE : 'false';

INTEGER : '0'|([1-9][0-9]*) ;
ID : [_$a-zA-Z][_$a-zA-Z0-9]* ;

WS : [ \t\n\r\f]+ -> skip ;
SINGLE_LINE_COMMENT : '//' (~[\r\n])* -> skip;
MULTI_LINE_COMMENT : '/*' .*? '*/' -> skip ;

program
    : (importDecl)* classDecl EOF
    ;

importDecl
    : IMPORT name+=ID('.'name+=ID)* ';'
    ;

classDecl
    : CLASS name=ID
        ('extends' superName=ID)?
        '{'
        varDecl*
        methodDecl*
        '}'
    ;

varDecl locals[boolean isPrivate=false]
    : (PRIVATE {$isPrivate=true;})? type name=ID ';'
    ;

type
    : type '[' ']' #ArrayType
    | type '...' #VarargType
    | id=( 'boolean' | 'byte' | 'char' | 'short' | 'int' | 'long' | 'float' | 'double' ) #PrimitiveType
    | id='void' #VoidType
    | id=ID #CustomType
    ;

param
    : type name=ID
    ;

methodDecl locals[boolean isPublic=false]
    : (PUBLIC {$isPublic=true;} | PRIVATE | PROTECTED)?
        (accessType=ACCESS_TYPE)?
        type name=ID
        '(' (param (','param)*)? ')'
        '{' varDecl* stmt* '}'
    ;

stmt
    : '{' stmt* '}' #StmtBody
    | IF '(' expr ')' stmt ELSE stmt #IfStmt
    | WHILE '(' expr ')' stmt #WhileStmt
    | expr ';' #DefaultStmt
    | expr '=' expr ';' #AssignStmt
    | 'return' expr ';' #ReturnStmt
    ;

args
    : expr ( ',' expr )* #FuncArgs
    ;

expr
    : '(' expr ')' #ParenExpr
    | op='!' expr #UnaryOp
    | expr '.length' # LengthExpr
    | expr '.' id=ID '(' args? ')' #FuncCall
    | expr '[' index=expr ']' #ArrayAccess
    | 'new' type '[' size=expr ']' # NewArray
    | 'new' id=ID '(' ')' # NewClass
    | '[' args ']' # ArrayInit
    | expr op=('*' | '/') expr #BinaryExpr
    | expr op=('+' | '-') expr #BinaryExpr
    | expr op=( '<' | '<=' | '>' | '>=' ) expr #ComparisonExpr
    | expr op='&&' expr #BooleanExpr
    | value=INTEGER #IntegerLiteral
    | value=TRUE #Boolean
    | value=FALSE #Boolean
    | name=ID #VarRefExpr
    ;
