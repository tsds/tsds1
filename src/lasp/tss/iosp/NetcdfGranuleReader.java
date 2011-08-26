package lasp.tss.iosp;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;

import ucar.ma2.Array;
import ucar.nc2.NetcdfFile;
import ucar.nc2.Variable;

public class NetcdfGranuleReader extends GranuleIOSP {

    private NetcdfFile _ncFile;
//    HashMap<String, Array> _varMap = new HashMap<String, Array>();
    
    @Override
    protected void readAllData() {
        String url = getProperty("url");
        try {
            _ncFile = NetcdfFile.open(url);
            System.out.println(""+_ncFile);
            
//            List<Variable> vars = _ncFile.getVariables();
//            for (Variable var : vars) {
//                String vname = var.getShortName();
//                Array array = var.read();
//                _varMap.put(vname, array);
//            }
            
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    protected Array getData(Variable var) {
        Array array = null;
        
        String vname = var.getFullNameEscaped();
        Variable v = _ncFile.findVariable(vname);
        try {
            array = v.read();
        } catch (IOException e) {
            // TODO Auto-generated catch block
            e.printStackTrace();
        }
//        Array array = _varMap.get(vname);
        
        return array;
    }
    
    @Override
    public void close() throws IOException {
        super.close();
        _ncFile.close();
    }

}
