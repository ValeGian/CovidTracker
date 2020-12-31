-module(aggregation_server).

-export([start_aggregation_server/0, start_aggregation_server/1, gimme_result/2, gimme_result/3]).

%versione server unico che esegue tutte le operazioni
%specificare l'operazione nella variabile Oper
aggregation_server_loop() ->
	receive
	{From, List, Oper} ->
		case Oper of
			sum -> Result = sum(List);
			avg -> Result = avg(List);
			standard_deviation -> Result = standard_deviation(List);
			_Else -> Result = -1
		end,
		io:format("SERVER: ~s: ~p~n", [Oper, Result]),
		From ! Result,
		aggregation_server_loop();
    _msg ->
		io:format("SERVER: received message ~p~n", [_msg])
	end.

start_aggregation_server() ->
	Serv_pid = spawn(fun() -> aggregation_server_loop() end ),
	%for process registration
	register(aggregation_server, Serv_pid),
	Serv_pid.
  
%versione server che esegue una sola operazione specificata all'avvio del server
%specificare l'operazione nella variabile Oper
aggregation_server_loop(Oper) ->
	receive
	{From, List} ->
		case Oper of
			sum -> Result = sum(List);
			avg -> Result = avg(List);
			standard_deviation -> Result = standard_deviation(List);
			_Else -> Result = -1
		end,
		io:format("SERVER: ~s: ~p~n", [Oper, Result]),
		From ! Result,
		aggregation_server_loop(Oper);
    _msg ->
		io:format("SERVER: received message ~p~n", [_msg])
	end.

start_aggregation_server(Oper) ->
	Serv_pid = spawn(fun() -> aggregation_server_loop(Oper) end ),
	%for process registration
	Name = list_to_atom(atom_to_list(Oper) ++ atom_to_list(server)),
	register(Name, Serv_pid),
	Serv_pid.
  
sum(L) -> sum(L, 0).
sum([H | T], Acc) -> sum(T, Acc + H);
sum([], Acc) -> Acc.

avg(L) -> avg(L, 0, 0).
avg([H | T], Sum, Num) -> avg(T, Sum + H, Num + 1);
avg([], Sum, Num) -> Sum / Num.

standard_deviation(L) -> standard_deviation(L, 0, 0, avg(L)).
standard_deviation([H | T], QuadraticDeviation, Num, Avg) -> standard_deviation(T, QuadraticDeviation + (H - Avg) * (H - Avg), Num + 1, Avg);
standard_deviation([], QuadraticDeviation, Num, _) -> math:sqrt(QuadraticDeviation / Num).

%funzioni per testare i server
gimme_result(Server, List, Oper) ->
  Server ! {self(), List, Oper},
  receive
    Result ->
      io:format("AT CLIENT: Result: ~p~n", [Result])
  after
    5000 ->
      io:format("AT CLIENT: No reply from server.~n")
  end.
  
gimme_result(Server, List) ->
  Server ! {self(), List},
  receive
    Result ->
      io:format("AT CLIENT: Result: ~p~n", [Result])
  after
    5000 ->
      io:format("AT CLIENT: No reply from server.~n")
  end.
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  