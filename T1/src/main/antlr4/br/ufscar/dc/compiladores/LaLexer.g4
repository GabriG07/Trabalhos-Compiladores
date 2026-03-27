lexer grammar LaLexer;

// Tokens reservados da linguagem LA.
PALAVRA_CHAVE: 'algoritmo' | 'declare' | 'constante' |'literal' | 'inteiro' | 'real' | 'var' | 'logico' | 'tipo' | 'registro'
    | 'falso' | 'verdadeiro' | 'leia' | 'escreva' | 'faca' | 'ate' | 'procedimento' | 'funcao' | 'retorne' | 'para' | 'se'
    | 'senao' | 'entao' | 'e'| 'nao' | 'ou' | 'caso' | 'seja' | 'enquanto' |'fim_registro' | 'fim_procedimento' | 'fim_funcao'
    | 'fim_se' | 'fim_para'| 'fim_caso' |'fim_enquanto' | 'fim_algoritmo';


fragment DIGITO: '0'..'9'; //bloco reutilizável nas regras que precisam reconhecer dígitos.

NUM_REAL : DIGITO+ '.' DIGITO+ ;
NUM_INT : DIGITO+ ;

CADEIA   : '"' ( ~('"' | '\n' | '\r') )* '"' ;
IDENT : ('a'..'z' | 'A'..'Z' | '_') ('a'..'z' | 'A'..'Z' | '0'..'9' | '_')* ;
COMENTARIO : '{' ~('}' | '\n' | '\r')* '}' { skip(); } ;
WS : ( ' ' | '\t' | '\r' | '\n' )+ { skip(); };
VIRGULA     : ',' ;
DPONTOS     : ':' ;
ABREPAR     : '(' ;
FECHAPAR    : ')' ;
PONTOPONTO  : '..' ;
PONTO       : '.' ;
ABREC       : '[' ;
FECHAC      : ']' ;
E_COMERCIAL : '&' ;
ATRIB       : '<-' ;
OP_REL      : '=' | '<>' | '>=' | '<=' | '>' | '<' ;
OP_ARIT     : '+' | '-' | '*' | '/' | '^';
OP_PERC     : '%' ;

//Regras de erro. Capturam situações léxicas inválidas.
COMENTARIO_NAO_FECHADO : '{' ~('}' | '\n' | '\r')* ;
CADEIA_NAO_FECHADA   : '"' ( ~('"' | '\n' | '\r') )*;
ERRO : . ; //Encontra qualquer token que não foi reconhecido por nenhuma regra anterior