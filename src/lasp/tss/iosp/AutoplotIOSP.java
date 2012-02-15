package lasp.tss.iosp;

import java.io.IOException;
import java.util.List;

import org.das2.util.monitor.NullProgressMonitor;
import org.virbo.dataset.DataSetUtil;
import org.virbo.dataset.QDataSet;
import org.virbo.datasource.DataSetURI;

import ucar.ma2.Array;
import ucar.ma2.DataType;
import ucar.nc2.Variable;

public class AutoplotIOSP extends GranuleIOSP {

    //TODO: aggregation example:
    //vap:ftp://nssdcftp.gsfc.nasa.gov/spacecraft_data/omni/omni2_$Y.dat?column=field17&timeFormat=$Y+$j+$H&time=field0&validMax=999&timerange=1972
    
    @Override
    protected void readAllData() {
        try {
            
            String base_uri = getProperty("vapuri");
            if (base_uri == null) throw new IllegalArgumentException("vapuri attribute must be specified");
            
            //Iterate over each variable
            List<Variable> vars = getVariables();
            for (Variable var : vars) {

                //Get the name of the source variable.
                String vname = var.getShortName();
                String ovname = getVariableXmlAttribute(vname, "srcName");
                String name = vname;
                if (ovname != null) name = ovname;
                
                //Construct vap uri for this variable
                String uri = base_uri + "&" + "column=" + name;
                QDataSet result = DataSetURI.getDataSource(uri).getDataSet(new NullProgressMonitor());
                
                //capture the length
                int n = result.length();
                if (vname.equals("time")) setLength(n);
                
                //read the data
                double[] data= new double[n];
                for ( int i=0; i<n; i++ ) {
                    data[i]= result.value(i);
                }

                //define the Variable's shape
                int[] shape = DataSetUtil.qubeDims(result);

                //Construct the ucar.ma2.Array and add it to the collection
                Array array = Array.factory(DataType.DOUBLE, shape, data);
                setArray(vname, array);
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
