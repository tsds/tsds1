; Enter the following commands at the IDL command line prompt.
; Data will be placed in matrix named D.
; Column labels will be placed in string array named L.

SVR  = "__SERVER__"
QS   = "__QUERYSTRING__"
FLT  = "filter=format_time%28yyyy+MM+dd+HH+mm+ss.S%29"

oUrl = OBJ_NEW('IDLnetUrl')
mc   = oUrl->Get(url=SVR+"?"+QS+"&thin(1)&stream=true", /string_array )
ds   = oUrl->Get(url=SVR+"?"+QS+"&"+FLT+"&stream=true&out=dat", /string_array )
D    = fltarr(size(ds,/DIMENSIONS),size(strsplit(ds[0]),/DIMENSIONS))
reads, ds, D

headers  = mc[0]
sheaders = strsplit(headers,',',/extract)
time     = sheaders[0]
L        = ['Year','Month','Day','Hour','Minute','Second',sheaders[1]]

help, D
print, 'has column labels'
print, L
