% Enter the following commands at the MATLAB command line prompt.
% Data will be placed in matrix named D.
% Column labels will be placed in cell array named L.

SVR  = '__SERVER__'
QS   = '__QUERYSTRING__'
FLT  = 'filter=format_time%28yyyy+MM+dd+HH+mm+ss.S%29'

ms = urlread([SVR,'?',QS,'&thin(1)&stream=true']);         % Read metadata as string
ds = urlread([SVR,'?',QS,'&',FLT,'&stream=true&out=dat']); % Read data as string
D  = str2num(ds); % Convert data to doubles

mc = regexp(ms,'\n','split');   % First line has column labels
ma = regexp(mc{1},',','split'); % Split on comma and keep non-time labels
L  = {'Year','Month','Day','Hour','Minute','Second',ma{2:end}};

whos D
fprintf('has column labels\n',size(D));
L

fprintf('tsdsfe.m: First timestamp: %s\n',datestr(D(1,1:6),31));
fprintf('tsdsfe.m: Second timestamp: %s\n',datestr(D(2,1:6),31));
fprintf('tsdsfe.m: Last timestamp: %s\n',datestr(D(end,1:6),31));
