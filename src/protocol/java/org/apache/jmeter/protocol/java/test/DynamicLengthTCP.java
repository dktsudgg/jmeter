package org.apache.jmeter.protocol.java.test;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketAddress;
import java.net.SocketException;
import java.net.UnknownHostException;
import java.nio.ByteBuffer;
import java.util.Arrays;
import java.util.Iterator;

import org.apache.jmeter.config.Arguments;
import org.apache.jmeter.protocol.java.sampler.AbstractJavaSamplerClient;
import org.apache.jmeter.protocol.java.sampler.JavaSamplerContext;
import org.apache.jmeter.samplers.Interruptible;
import org.apache.jmeter.samplers.SampleResult;
import org.apache.jmeter.threads.JMeterVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import javax.xml.bind.DatatypeConverter;

public class DynamicLengthTCP extends AbstractJavaSamplerClient implements Serializable, Interruptible {

    private static final Logger LOG = LoggerFactory.getLogger(DynamicLengthTCP.class);

    private static final long serialVersionUID = 80001L;

    private String ip = "127.0.0.1";
    private int port = 9123;
    private int connectTimeout = 0;
    private int responseTimeout = 0;
    private boolean isReuseConnection = true;
    private boolean isNoDelay = false;
    private int SoLinger = 0;
    private int sizeHeader = 0;
    private int offsetBodyLength = 0;
    private int sizeBodyLength = 0;
    
    private static final SingletonSocketMap tsocket = SingletonSocketMap.getInstance();

    /**
     * Default constructor for <code>JavaTest</code>.
     *
     * The Java Sampler uses the default constructor to instantiate an instance
     * of the client class.
     */
    public DynamicLengthTCP() {
        LOG.debug(whoAmI() + "\tConstruct");
    }

    /*
     * Utility method to set up all the values
     */
    private void setupValues(JavaSamplerContext context) {    	
    	ip = context.getParameter("Host IP", "127.0.0.1");
        port = context.getIntParameter("Port", 9123);
        connectTimeout = context.getIntParameter("Connect Timeout", 0);
        responseTimeout = context.getIntParameter("Response Timeout", 0);
        isReuseConnection = (context.getParameter("Re-use connect", "true").compareTo("true")==0) ? true : false;
        isNoDelay = (context.getParameter("Set NoDelay", "false").compareTo("true")==0) ? true : false;
        SoLinger = context.getIntParameter("SO_LINGER", 0);
        sizeHeader = context.getIntParameter("Size of response header", 8);
        offsetBodyLength = context.getIntParameter("Offset of response body length", 6);
        sizeBodyLength = context.getIntParameter("Size of response body length", 2);
    }

    /**
     * �ʱ� �ѹ��� ȣ��� . �α��ΰ� ���� �ѹ��� ȣ��Ǵ� ���� ����
     * Do any initialization required by this client.
     *
     * There is none, as it is done in runTest() in order to be able to vary the
     * data for each sample.
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     */
    @Override
    public void setupTest(JavaSamplerContext context) {
        if (LOG.isDebugEnabled()) {
            LOG.debug(whoAmI() + "\tsetupTest()");
            listParameters(context);
        }
    }

    /**
     * GUI ȭ���� Parameter �� ǥ��
     * Provide a list of parameters which this test supports. Any parameter
     * names and associated values returned by this method will appear in the
     * GUI by default so the user doesn't have to remember the exact names. The
     * user can add other parameters which are not listed here. If this method
     * returns null then no parameters will be listed. If the value for some
     * parameter is null then that parameter will be listed in the GUI with an
     * empty value.
     *
     * @return a specification of the parameters used by this test which should
     *         be listed in the GUI, or null if no parameters should be listed.
     */
    @Override
    public Arguments getDefaultParameters() {
        Arguments params = new Arguments();
        params.addArgument("Host IP", "127.0.0.1");
        params.addArgument("Port", "9123");
        params.addArgument("Connect Timeout", "0");
        params.addArgument("Response Timeout", "0");
        params.addArgument("Re-use connect", "true");
        params.addArgument("Set NoDelay", "false");
        params.addArgument("SO_LINGER", "0");
        params.addArgument("Size of response header", "8");
        params.addArgument("Offset of response body length", "6");
        params.addArgument("Size of response body length", "2");
        return params;
    }

    /**
     * �ݺ��Ǵ� ���� ����. 
     * Perform a single sample.<br>
     * In this case, this method will simply sleep for some amount of time.
     *
     * This method returns a <code>SampleResult</code> object.
     *
     * <pre>
     *
     *  The following fields are always set:
     *  - responseCode (default &quot;&quot;)
     *  - responseMessage (default &quot;&quot;)
     *  - label (set from LABEL_NAME parameter if it exists, else element name)
     *  - success (default true)
     *
     * </pre>
     *
     * The following fields are set from the user-defined parameters, if
     * supplied:
     *
     * <pre>
     * -samplerData - responseData
     * </pre>
     *
     * @see org.apache.jmeter.samplers.SampleResult#sampleStart()
     * @see org.apache.jmeter.samplers.SampleResult#sampleEnd()
     * @see org.apache.jmeter.samplers.SampleResult#setSuccessful(boolean)
     * @see org.apache.jmeter.samplers.SampleResult#setSampleLabel(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseCode(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseMessage(String)
     * @see org.apache.jmeter.samplers.SampleResult#setResponseData(byte[])
     * @see org.apache.jmeter.samplers.SampleResult#setDataType(String)
     *
     * @param context
     *            the context to run with. This provides access to
     *            initialization parameters.
     *
     * @return a SampleResult giving the results of this sample.
     */
    @Override
    public SampleResult runTest(JavaSamplerContext context) {
        setupValues(context);
        
        //LOG.warn("start urlmgrTest.......");
        
        SampleResult results = new SampleResult();
        Socket sock = null;
        
        try {
            try {
                sock = getSocket();
            } finally {
                results.connectEnd();
            }
            if (sock == null) {
                results.setResponseCode("500"); //$NON-NLS-1$
                results.setResponseMessage("cannot connect " + ip);
            } else {
            	DataOutputStream os = new DataOutputStream(sock.getOutputStream());
            	DataInputStream is = new DataInputStream(sock.getInputStream());
            	
                JMeterVariables vars = context.getJMeterVariables();
                String req =vars.get("RequestData");
                byte[] reqData = DatatypeConverter.parseHexBinary(req);
                //LOG.info("Request size:" + reqData.length);
                
                results.sampleStart();
                
                // send
                os.write(reqData);
                
                // receive header
                byte[] resHeader = new byte[sizeHeader];
                is.readFully(resHeader, 0, sizeHeader);
                
                //body size
                int sizeBody = getBodySize(resHeader);
                sizeBody = sizeBody + 1;    // 내 패킷은 Header-protocol-data 구조인데 나는 Header가 나타내는 값이 protocol을 제외한 data의 길이이므로 protocol(1바이트)만큼 추가해줌..
                
                byte[] res = new byte[sizeHeader + sizeBody];
                System.arraycopy(resHeader, 0, res, 0, sizeHeader);
                
                if (sizeBody > 0) {
                    byte[] resBody = new byte[sizeBody];
                    is.readFully(resBody, 0, sizeBody);
                    
                    System.arraycopy(resBody, 0, res, sizeHeader, sizeBody);
                }
                
                //LOG.info("Response size:" + res.length);
                
                // Calculate response time
                results.sampleEnd();
                
                results.setResponseData(res);
                results.setResponseCodeOK();
                results.setResponseMessage("OK");
                results.setSuccessful(true);
            }
        } catch (Exception ex) {
            LOG.error("", ex);
            if (sock!=null) closeSocket(sock);
            
            results.setResponseCode("500");
            results.setResponseMessage("NOK");
            results.setSuccessful(false);
        } finally {
            results.setDataType(SampleResult.BINARY);
        	
            // Calculate response time
            //results.sampleEnd();
        }

        if (LOG.isDebugEnabled()) {
            LOG.debug(whoAmI() + "\trunTest()" + "\tTime:\t" + results.getTime());
            listParameters(context);
        }

        return results;
    }

    /**
     * Dump a list of the parameters in this context to the debug log.
     * Should only be called if debug is enabled.
     *
     * @param context
     *            the context which contains the initialization parameters.
     */
    private void listParameters(JavaSamplerContext context) {
        Iterator<String> argsIt = context.getParameterNamesIterator();
        while (argsIt.hasNext()) {
            String name = argsIt.next();
            LOG.debug(name + "=" + context.getParameter(name));
        }
    }

    /**
     * Generate a String identifier of this test for debugging purposes.
     *
     * @return a String identifier for this test instance
     */
    private String whoAmI() {
        StringBuilder sb = new StringBuilder();
        sb.append(Thread.currentThread().toString());
        sb.append("@");
        sb.append(Integer.toHexString(hashCode()));
        return sb.toString();
    }

    @Override
    public boolean interrupt() {
    	Thread.currentThread().interrupt();
    	return true;
    }
    /* Implements JavaSamplerClient.teardownTest(JavaSamplerContext) */
    @Override
    public void teardownTest(JavaSamplerContext context) {
    	tsocket.release();
    }
    
    
    private void closeSocket(Socket sock) {
    	tsocket.set(null);
        if (sock != null) {
            LOG.debug("{} Closing connection {}", this, sock);
            try {
            	sock.close();
            } catch (IOException e) {
                LOG.warn("Error closing socket {}", e);
            }
        }
    }
    
    private Socket getSocket() {
        Socket sock = null;
        if (isReuseConnection) {
        	sock = tsocket.get();
            if (sock != null) {
                LOG.debug("{} Reusing connection {}", this, sock);
            }
        }

        if (sock == null) {
            // Not in cache, so create new one and cache it
            try {
                SocketAddress sockaddr = new InetSocketAddress(ip, port);
                sock = new Socket();
                if (SoLinger > 0){
                	sock.setSoLinger(true, SoLinger);
                }
                sock.connect(sockaddr, connectTimeout);
                if(LOG.isDebugEnabled()) {
                	LOG.debug("Created new connection {}", sock);
                }
                
                tsocket.set(sock);
            } catch (UnknownHostException e) {
                LOG.warn("Unknown host for {}", ip, e);
                return null;
            } catch (IOException e) {
                LOG.warn("Could not create socket for {}", ip, e);
                return null;
            }     
        }
  
        try {
        	sock.setSoTimeout(responseTimeout);
        	sock.setTcpNoDelay(isNoDelay);
            if(LOG.isDebugEnabled()) {
            	LOG.debug("{} Timeout={}, NoDelay={}", this, responseTimeout, isNoDelay);
            }
        } catch (SocketException se) {
        	LOG.warn("Could not set timeout or nodelay for {}", ip, se);
        }
        return sock;
    }
    
    private int getBodySize(byte[] header) throws Exception {
    	int size = 0;
    	
    	try {
	    	byte[] bytes = new byte[sizeBodyLength];
	    	bytes = Arrays.copyOfRange(header, offsetBodyLength, offsetBodyLength + sizeBodyLength );
	    	size = bytesToInt(bytes);
        } catch (Exception e) {
        	LOG.warn("response header parsing error offset: {}, size: {}", offsetBodyLength, sizeBodyLength);
        	throw new Exception("response header parsing error");
        }	    	
    	
    	return size;
    }
    
    private int bytesToInt(byte[] bytes) throws Exception {
    	int size = 0;
    	try {
        	switch(bytes.length) {
        	case 1:
        		size = (int)ByteBuffer.wrap(bytes).getChar();
        		break;
        	case 2:
        		size = (int)ByteBuffer.wrap(bytes).getShort();
        		break;
        	case 4:
        		size = ByteBuffer.wrap(bytes).getInt();
        		break;
//        	case 8:
//        		size = (int)ByteBuffer.wrap(bytes).getDouble();
//        		break;
        	default:
        		throw new Exception("Not support yet. size: "+ bytes.length);
        	}    		
    	} catch(Exception e) {
    		throw e;
    	}
    	
    	return size;
    }
}
