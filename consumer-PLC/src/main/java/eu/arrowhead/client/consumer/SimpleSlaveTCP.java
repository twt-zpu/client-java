package eu.arrowhead.client.consumer;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Observer;

import com.intelligt.modbus.jlibmodbus.Modbus;
import com.intelligt.modbus.jlibmodbus.data.DataHolder;
import com.intelligt.modbus.jlibmodbus.data.ModbusCoils;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataAddressException;
import com.intelligt.modbus.jlibmodbus.exception.IllegalDataValueException;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlave;
import com.intelligt.modbus.jlibmodbus.slave.ModbusSlaveFactory;
import com.intelligt.modbus.jlibmodbus.tcp.TcpParameters;
import com.intelligt.modbus.jlibmodbus.utils.DataUtils;
import com.intelligt.modbus.jlibmodbus.utils.FrameEvent;
import com.intelligt.modbus.jlibmodbus.utils.FrameEventListener;
import com.intelligt.modbus.jlibmodbus.utils.ModbusSlaveTcpObserver;
import com.intelligt.modbus.jlibmodbus.utils.TcpClientInfo;

public class SimpleSlaveTCP {
	private ModbusSlave slave;
	private TcpParameters tcpParameters = new TcpParameters();
	private ModbusCoils hc = new ModbusCoils(20);
	private MyOwnDataHolder dh = new MyOwnDataHolder();
	private ModbusData measurement = new ModbusData();
	
	public void main(String[] argv){
		startSlave();
	}
	
	
	public void startSlave(){
		try{
			setSlave();
			slave.listen();
		} catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            e.printStackTrace();
        }
	}
	
	public ModbusCoils getCoils(){
		return hc;
	}
	
	public void setCoils(ModbusCoils hc){
		this.hc = hc;
	}
	
	private void setTCPConnection() throws UnknownHostException{
		byte[] address = {10, 12, 90, 10};
	    tcpParameters.setHost(InetAddress.getByAddress(address));
	    tcpParameters.setKeepAlive(true);
	    tcpParameters.setPort(502);
	}
	
	private void setSlave() throws IllegalDataAddressException, IllegalDataValueException, UnknownHostException{
		setTCPConnection();
		slave = ModbusSlaveFactory.createModbusSlaveTCP(tcpParameters);
		slave.setServerAddress(Modbus.TCP_DEFAULT_ID);
        slave.setBroadcastEnabled(true);
        slave.setReadTimeout(1000);
        Modbus.setLogLevel(Modbus.LogLevel.LEVEL_DEBUG);
        setDataHolder();
        setFrameEventListener();
        setObserver();
        slave.setServerAddress(1);
	}
	
	private void setDataHolder() throws IllegalDataAddressException, IllegalDataValueException{
		dh.addEventListener(new ModbusEventListener() {
			@Override
            public void onWriteToSingleCoil(int address, boolean value) {
                System.out.print("onWriteToSingleCoil: address " + address + ", value " + value + "\n");
            }

            @Override
			public void onReadMultipleCoils(int address, int quantity) throws IllegalDataAddressException, IllegalDataValueException{
				System.out.print("onReadMultipleCoils: address " + address + ", quantity " + quantity + "\n");
				HashMap<Integer, Boolean> valuesMap = new HashMap<Integer, Boolean>();
				valuesMap = measurement.getEntry().getCoilsInput();
				for(int index = 0; index <quantity; index++){
					int offsetIndex = address + index;
					hc.set(offsetIndex, valuesMap.get(offsetIndex));
				}
				// slave.getDataHolder().setCoils(hc);
			}
            
            @Override
            public void onWriteToMultipleCoils(int address, int quantity, boolean[] values) {
                System.out.print("onWriteToMultipleCoils: address " + address + ", quantity " + quantity + "\n");
                measurement.getEntry().setCoilsOutput(address, values);
            }

            @Override
            public void onWriteToSingleHoldingRegister(int address, int value) {
                System.out.print("onWriteToSingleHoldingRegister: address " + address + ", value " + value + "\n");
            }

            @Override
            public void onWriteToMultipleHoldingRegisters(int address, int quantity, int[] values) {
                System.out.print("onWriteToMultipleHoldingRegisters: address " + address + ", quantity " + quantity + "\n");
            }
        });
        slave.setDataHolder(dh);
        // hc.set(0, Boolean.TRUE);
        // hc.set(1, Boolean.TRUE);
        slave.getDataHolder().setCoils(hc);
	}
	
	private void setFrameEventListener(){
		FrameEventListener listener = new FrameEventListener() {
            @Override
            public void frameSentEvent(FrameEvent event) {
                System.out.println("frame sent " + DataUtils.toAscii(event.getBytes()));
            }

            @Override
            public void frameReceivedEvent(FrameEvent event) {
                System.out.println("frame recv " + DataUtils.toAscii(event.getBytes()));
            }
        };
        slave.addListener(listener);
	}
	
	private void setObserver(){
		Observer o = new ModbusSlaveTcpObserver() {
            @Override
            public void clientAccepted(TcpClientInfo info) {
                System.out.println("Client connected " + info.getTcpParameters().getHost());
            }

            @Override
            public void clientDisconnected(TcpClientInfo info) {
                System.out.println("Client disconnected " + info.getTcpParameters().getHost());
            }
        };
        slave.addObserver(o);
	}
	
	public interface ModbusEventListener {
        void onWriteToSingleCoil(int address, boolean value);
        
        void onReadMultipleCoils(int address, int quantity) throws IllegalDataAddressException, IllegalDataValueException;

        void onWriteToMultipleCoils(int address, int quantity, boolean[] values);

        void onWriteToSingleHoldingRegister(int address, int value);

        void onWriteToMultipleHoldingRegisters(int address, int quantity, int[] values);
    }

    public static class MyOwnDataHolder extends DataHolder {

        final List<ModbusEventListener> modbusEventListenerList = new ArrayList<ModbusEventListener>();

        public MyOwnDataHolder() {
            // setCoils(new ModbusCoils(1024));
        }

        public void addEventListener(ModbusEventListener listener) {
            modbusEventListenerList.add(listener);
        }

        public boolean removeEventListener(ModbusEventListener listener) {
            return modbusEventListenerList.remove(listener);
        }

        @Override
        public void writeHoldingRegister(int offset, int value) throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToSingleHoldingRegister(offset, value);
            }
            super.writeHoldingRegister(offset, value);
        }

        @Override
        public void writeHoldingRegisterRange(int offset, int[] range) throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToMultipleHoldingRegisters(offset, range.length, range);
            }
            super.writeHoldingRegisterRange(offset, range);
        }

        @Override
        public void writeCoil(int offset, boolean value) throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToSingleCoil(offset, value);
            }
            super.writeCoil(offset, value);
        }

        @Override
        public void writeCoilRange(int offset, boolean[] range) throws IllegalDataAddressException, IllegalDataValueException {
            for (ModbusEventListener l : modbusEventListenerList) {
                l.onWriteToMultipleCoils(offset, range.length, range);
            }
            super.writeCoilRange(offset, range);
        }
        
        @Override
        public boolean[] readCoilRange(int offset, int quantity) throws IllegalDataAddressException, IllegalDataValueException{
        	boolean[] values = super.readCoilRange(offset, quantity);
        	for (ModbusEventListener l : modbusEventListenerList) {
                l.onReadMultipleCoils(offset, quantity);
            }
            return values;
        }
    }
	
}
