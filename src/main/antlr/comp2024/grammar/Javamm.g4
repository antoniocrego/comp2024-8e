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
RETURN : 'return' ;

CLASS : 'class' ;

PUBLIC : 'public' ;

LENGTH : 'length' ;
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
        '{'
        varDecl*
        methodDecl*
        '}'
    ;

varDecl
    : type name=ID ';'
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
    : (PUBLIC {$isPublic=true;})?
        ACCESS_TYPE?
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
    | RETURN expr ';' #ReturnStmt
    ;

args
    : expr ( ',' expr )*
    ;

expr
    : '(' expr ')' #ParenExpr
    | op='!' expr #UnaryOp
    | expr '.' LENGTH # LengthExpr
    | expr '.' ID '(' args? ')' #FuncCall
    | expr '[' index=expr ']' #ArrayAccess
    | 'new' type '[' size=expr ']' # NewArray
    | 'new' ID '(' ')' # NewClass //this doesn't allow passing parameters to the constructor
    | '[' args ']' # ArrayInit
    | expr op=('*' | '/') expr #BinaryOp
    | expr op=('+' | '-') expr #BinaryOp
    | expr op=( '<' | '<=' | '>' | '>=' ) expr #BinaryOp
    | expr op='&&' expr #BinaryOp
    | value=INTEGER #IntegerLiteral
    | name=ID #VarRefExpr
    ;
