function [D,L] = tsdsfe(list1)

%TODO: Optimizations:
% * Parallel downloads using http://aurora.gmu.edu/svn/m-rsw/java/asyncdl/
%   if URL list has more than one value.
% * See github scripts/tsdsgen.m for other optimizations.
% * If out=bin is detected in url, try different method. (save to ram disk,
%   for example.)

s    = urlread(list);
urls = regexp(s,'\n','split');

D  = [];
Nu = length(urls);
N  = 0;
Nl = 0;
tic;
for i = 1:length(urls)
  fprintf('tsdsfe.m: Downloading part %d/%d.  ',i,length(urls));
  d = urlread(urls{i});

  df = str2num(d);
  N(i)  = size(df,1);

  % Pre-allocate.  Guess final size.
  if (i == 1)
    fprintf('\ntsdsfe.m: Allocating %.2f MB array.\n',...
	    (8*size(df,1)*Nu*size(df,2))/1e6);
    D = zeros(size(df,1)*Nu,size(df,2));
    fprintf('tsdsfe.m: First timestamp: %s\n',datestr(df(1,1:6),31));
    fprintf('tsdsfe.m: Second timestamp: %s\n',datestr(df(2,1:6),31));
  end;

  fprintf('tsdsfe.m: Converting and appending.\n',i,Nu);
  D(Nl+1:Nl+N(i),:) = df;
  Nl = N(i);
end
B = 8*prod(size(D))/1e6;
te = toc;
fprintf('tsdsfe.m: Done.  %.2f MB in %.2f sec (%.2f MB/s)\n',B,te,B/te);
fprintf('tsdsfe.m: Last timestamp: %s\n',datestr(df(end,1:6),31));
D = D(1:sum(N),:); % Remove zeros
Tl = {'Year','Month','Day','Hour','Minute','Second'}
Dl = {};
