-module(aggregation_server).

-export([start_avgserver/0, gimme_avg/2, sum/1, avg/1]).
  
%% Write a server that receives one number per request, and at each request
%% prints out the average of all the numbers received so far and replies
%% back with such a result.
%% Write a "client" function to test the server.


% the server Pid is obtained by calling start_avgserv()
% The message to the server: {self(), Mynumber}
% Simple function to test the server: gimme_avg/1
% The server is registered as "avg_server".

avg_server_loop() ->
  receive
    {From,List} ->
      Avg = avg(List),
      io:format("AT SERVER: Current average: ~p~n", [Avg]),
      From ! Avg,
      avg_server_loop();
    _msg ->
      io:format("AT SERVER: received message ~p~n", [_msg])

  end.

start_avgserver() ->
  Serv_pid = spawn(fun() -> avg_server_loop() end ),
  %for process registration
  register(avg_server, Serv_pid),
  Serv_pid.

gimme_avg(Server, NextNum) ->
  Server ! {self(),NextNum},
  receive
    Avg ->
      io:format("AT CLIENT: Current average: ~p~n", [Avg])
  after
    5000 ->
      io:format("AT CLIENT: No reply from server.~n")
  end.
  
sum(L) -> sum(L, 0).
sum([H | T], Acc) -> sum(T, Acc + H);
sum([], Acc) -> Acc.

avg(L) -> avg(L, 0, 0).
avg([H | T], Sum, Num) -> avg(T, Sum + H, Num + 1);
avg([], Sum, Num) -> Sum / Num.
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  
  