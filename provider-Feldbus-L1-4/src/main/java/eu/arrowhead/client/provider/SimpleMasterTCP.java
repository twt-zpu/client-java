package eu.arrowhead.client.provider;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.List;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

public class SimpleMasterTCP {
	private TcpParameters tcpParameters = new TcpParameters();
	private ModbusMaster master;
	private FeldbusCouplerLMeasurement measurement; 
	private int slaveId = 1;
	private int offset = 0;
	private int quantity = 1;
	
	public SimpleMasterTCP(){
		
	}
	
 	public List<Boolean> readMaster(int offset, int quantity){
		this.offset = offset;
		this.quantity = quantity;
		
		List<Boolean> coilsList = new ArrayList<Boolean>();
		try{
			if (!master.isConnected()){
				master.connect();
			}
			boolean[] coilsArray = master.readCoils(slaveId, offset, 9);
			int index = 1;
			for (boolean coil : coilsArray){
				if (9 < index++){
					break;
				}
				coilsList.add(coil);
			}
		} catch (ModbusProtocolException e) {
            e.printStackTrace();
        } catch (ModbusNumberException e) {
            e.printStackTrace();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        } finally {
            try {
                master.disconnect();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            }
        }
		return coilsList;
	}
	
	public List<Boolean> readMaster(){
		List<Boolean> coilsList = new ArrayList<Boolean>();
		coilsList = readMaster(0, 9);
		boolean[] coilsArray = new boolean[coilsList.size()];
		int index = 0;
		for (Boolean coil : coilsList){
			coilsArray[index++] = coil;
		}
		measurement.entry.setInput(coilsArray);
		return coilsList;
	}
	
	public void writeMaster(int startAddress){
		if (!measurement.isExist()){
			return ;
		}
		try{
			if (!master.isConnected()){
				master.connect();
			}
			List<Boolean> coilsList = new ArrayList<>();
			coilsList = measurement.entry.getOutput();
			boolean[] coilsArray = new boolean[coilsList.size()];
			int index = 0;
			for (Boolean coil : coilsList){
				coilsArray[index++] = coil;
			}
			master.writeMultipleCoils(slaveId, startAddress, coilsArray);
			
		} catch (ModbusProtocolException e) {
            e.printStackTrace();
        } catch (ModbusNumberException e) {
            e.printStackTrace();
        } catch (ModbusIOException e) {
            e.printStackTrace();
        } finally {
            try {
                master.disconnect();
            } catch (ModbusIOException e) {
                e.printStackTrace();
            }
        }
	}
	
	public void writeMaster(){
		writeMaster(512);
	}
	
	private void setTCPParameters(){
		try{
			byte[] address = {10, 12, 90, 14};
			tcpParameters.setHost(InetAddress.getByAddress(address));
	        tcpParameters.setKeepAlive(true);
	        tcpParameters.setPort(502);
		} catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public void setModbusMaster(){
		setTCPParameters();
		master = ModbusMasterFactory.createModbusMasterTCP(tcpParameters);
        Modbus.setAutoIncrementTransactionId(true);
	}
	
	
}
