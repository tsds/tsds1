; IDL class to read data from the Time Series Server.
; Sample usage:
;   tss = OBJ_NEW('tss_reader', baseurl='http://lasp.colorado.edu/lisird/tss/')
;   data = tss->read_data(dataset='sorce_tsi_24hr', ce='&time>2009-01-01&replace_missing(NaN)')
;
;TODO: Make ce a friendly string array? see what opendap client does.
;      Make another arg for an array of projected variables
;TODO: ADD SUPPORT FOR SPECTRAL DATA, look into Barry's byte array to struct code
;-----------------------------------------------------------------------------
; Constructor
function tss_reader::init, baseurl=baseurl, dataset=dataset, ce=ce
  if n_elements(baseurl) gt 0 then self.baseurl = baseurl
  if n_elements(dataset) gt 0 then self.dataset = dataset
  if n_elements(ce)      gt 0 then self.ce = ce
    
  return,1 ;status?
end

;-----------------------------------------------------------------------------
; Read the data
function tss_reader::read_data, baseurl=baseurl, dataset=dataset, ce=ce
  if n_elements(baseurl) gt 0 then self.baseurl = baseurl
  if n_elements(dataset) gt 0 then self.dataset = dataset
  if n_elements(ce)      gt 0 then self.ce = ce
  ;TODO: error if baseurl or dataset not defined
  
  struct = self->make_structure()
  
  url = self->make_url('bin');
  ourl = OBJ_NEW('IDLnetUrl') 
  bytes = ourl->Get(url=url, /buffer) ;byte array
  OBJ_DESTROY, ourl 
  
  n = n_elements(bytes)/8 ;number of doubles in the data
  nvar = n_tags(struct) ;the structure will have one tag for each variable
  ntim = n/nvar ;the number of time samples

  d = double(bytes, 0, n) ;convert bytes to doubles
  d = REFORM(d, nvar, ntim, /OVERWRITE) ;reform as 2D array for convenient access
  
  data = replicate(struct, ntim)

  ;Populate the data structures
  for itim=0,ntim-1 do begin
    for ivar=0,nvar-1 do begin
      data[itim].(ivar) = d[ivar,itim]
    endfor
  endfor
  
  return, data
end

;-----------------------------------------------------------------------------
; Get the OPeNDAP DDS as a string
function tss_reader::get_dds
  url = self->make_url('dds');
  ourl = OBJ_NEW('IDLnetUrl')
  dds = ourl->Get(url=url, /string)
  OBJ_DESTROY, ourl 
  
  return, dds
end

;-----------------------------------------------------------------------------
; Create an IDL structure based on the data set's OPeNDAP DDS.
function tss_reader::make_structure
  cmd = "struct = CREATE_STRUCT(name='"+self.dataset+"'"

  ;parse DDS to get variable names
  ;TODO: assumes scalar time series with double only, for now
  dds = self->get_dds()
  nvar = n_elements(dds) - 4 ;don't count surrounding structure
  for ivar=0,nvar-1 do begin
    ss = strsplit(dds[ivar+2], /extract)
    ;TODO: use data type, assume Float64 for now
    vname = ss[1] 
    vname = strmid(vname, 0, strlen(vname)-1) ;remove trailing ";", TODO: use regex?
    cmd += ", '"+vname+"', 0d"
  endfor

  cmd += ")"

  r = execute(cmd)
  ;TODO: test for error 

  return, struct
end

;-----------------------------------------------------------------------------
; Construct the TSS OPeNDAP URL for the given suffix.
function tss_reader::make_url, suffix
  url = self.baseurl + self.dataset + "."+suffix
  if n_elements(self.ce) gt 0 then url += "?" + self.ce
  return, url
end

;-----------------------------------------------------------------------------
; Time Series Server Reader class definition.
pro tss_reader__define 

  self = {tss_reader,  $
          dataset:'',  $
          baseurl:'',  $
          ce:''}

end
