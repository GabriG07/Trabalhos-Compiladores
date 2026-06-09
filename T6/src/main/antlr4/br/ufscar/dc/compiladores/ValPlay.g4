grammar ValPlay;

// ---------------- PARSER ----------------

// Um plano declara o mapa, a composicao de 5 agentes e uma ou mais execuções
plano: 'mapa' IDENT composicao execute* EOF;

// A composicao lista os agentes escolhidos (o nome de cada agente e um IDENT)
composicao: 'composicao' '{' IDENT* '}';

// Um execute descreve, para um site, a sequencia de ações do time
execute: 'execute' 'site' IDENT '{' acao* '}';

// Ações possiveis. As alternativas sao rotuladas (#) para facilitar o visitor
acao
    : IDENT 'usa' CADEIA ('em' CADEIA (',' CADEIA)*)?        # acaoUsa
    | IDENT 'ultimate' CADEIA ('em' CADEIA (',' CADEIA)*)?   # acaoUltimate
    | IDENT 'entra'                                          # acaoEntra
    | IDENT 'planta'                                         # acaoPlanta
    ;

// ---------------- LEXER ----------------

// Cadeia entre aspas: nomes de habilidades e posicoes do mapa
CADEIA   : '"' ~('"' | '\r' | '\n')* '"' ;

// Identificador: nomes de agentes e mapas
IDENT    : [A-Za-z] [A-Za-z0-9_/]* ;

//Ignora comentários e quebras de linha
COMENTARIO : '//' ~[\r\n]* { skip(); } ;
WS : ( ' ' | '\t' | '\r' | '\n' )+ { skip(); };

//Regras de erro. Capturam situações léxicas inválidas.
CADEIA_NAO_FECHADA   : '"' ( ~('"' | '\n' | '\r') )*;
ERRO : . ; //Encontra qualquer token  que não foi reconhecido por nenhuma regra anterior