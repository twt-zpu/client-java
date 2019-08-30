package eu.arrowhead.client.provider;

import java.net.InetAddress;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.exception.ModbusIOException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusNumberException;
import com.intelligt.modbus.jlibmodbus.exception.ModbusProtocolException;
import com.intelligt.modbus.jlibmodbus.master.ModbusMaster;
import com.intelligt.modbus.jlibmodbus.master.ModbusMasterFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;

import eu.arrowhead.client.common.model.ModbusMeasurement;

public class SimpleMasterTCP {
	private TcpParameters tcpParameters = new TcpParameters();
	private ModbusMaster master;
	private ModbusData measurement; 
	private int slaveId = 1;
	private int offset = 0;
	private int quantity = 1;
	
	
 	public List<Boolean> readMasterCoils(int offset, int quantity){
		this.offset = offset;
		this.quantity = quantity;
		
		List<Boolean> coilsList = new ArrayList<Boolean>();
		try{
			if (!master.isConnected()){
				master.connect();
			}
			boolean[] coilsArray = master.readCoils(slaveId, offset, quantity);
			for (int index = 0; index < quantity; index++){
				int offsetIndex = offset + index;
				measurement.getEntry().setCoilInput(offsetIndex, coilsArray[index]);
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
		return readMasterCoils(0, 9);
	}
	
	public void writeMasterCoils(){
		if (!measurement.isExist()){
			return ;
		}
		try{
			if (!master.isConnected()){
				master.connect();
			}
			HashMap<Integer, Boolean> coilsMap = new HashMap<Integer, Boolean>();
			coilsMap = measurement.getEntry().getCoilsOutput();
			for (Map.Entry<Integer, Boolean> entry : coilsMap.entrySet()){
				int address = entry.getKey();
				boolean value = entry.getValue();
				master.writeSingleCoil(slaveId, address, value);
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
	}
	
	public void writeMaster(){
		writeMasterCoils();
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
