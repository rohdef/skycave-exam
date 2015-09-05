package cloud.cave.client;

import static org.junit.Assert.*;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.Cave;
import cloud.cave.doubles.LocalMethodCallClientRequestHandler;
import cloud.cave.doubles.SaboteurCRHDecorator;
import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.StandardInvoker;

import org.junit.Before;
import org.junit.Test;
import org.apache.commons.io.IOUtils;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class TestAppserverDisconnect {
    private Cave cave;
    private SaboteurCRHDecorator saboteur;

    @Before
    public void setup() {
        // Create the server tier
        Cave caveRemote = CommonCaveTests.createTestDoubledConfiguredCave();

        Invoker srh = new StandardInvoker(caveRemote);

        ClientRequestHandler properCrh = new LocalMethodCallClientRequestHandler(srh);

        // Decorate the proper CRH with one that simulate errors, i.e. a Saboteur
        saboteur = new SaboteurCRHDecorator(properCrh);

        cave = new CaveProxy(saboteur);
    }
    @Test
    public void shouldNotCrashWithException(){
        InputStream inputStream = IOUtils.toInputStream("n\ns\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", System.out, inputStream);

        saboteur.throwNextTime("Disconnect from server, please restart");
        try {
            commandInterpreter.readEvalLoop();
            assertTrue(true);
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void shouldAskUserToDisconnectWhenServerUnresponsive(){
        InputStream inputStream = IOUtils.toInputStream("n\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", System.out, inputStream);
        saboteur.throwNextTime("Disconnect from server, please restart");
        try {
            commandInterpreter.readEvalLoop();
            assertTrue(true);
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }
}
