package lasp.tss.iosp;

import java.io.IOException;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetcdfGranuleReader extends GranuleIOSP {

    private NetcdfFile _netcdfFile;
    
    @Override
    protected void readAllData() {
        String url = getURL();
        
        try {
            _netcdfFile = NetcdfFile.open(url);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Array getData(Variable var) {
        Array array = null;
        
        String vname = var.getFullNameEscaped();
        Variable v = _netcdfFile.findVariable(vname);
        try {
            array = v.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        
        return array;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        _netcdfFile.close();
    }

}
