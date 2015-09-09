package cloud.cave.client;

import cloud.cave.common.CommonCaveTests;
import cloud.cave.domain.Cave;
import cloud.cave.doubles.LocalMethodCallClientRequestHandler;
import cloud.cave.doubles.SaboteurCRHDecorator;
import cloud.cave.ipc.ClientRequestHandler;
import cloud.cave.ipc.Invoker;
import cloud.cave.server.StandardInvoker;
import com.google.common.base.Predicates;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.apache.commons.io.IOUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.PrintStream;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.PortUnreachableException;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class TestAppserverDisconnect {
    private Cave cave;
    private SaboteurCRHDecorator saboteur;
    private String disconnectedMessage = "*** Sorry - I cannot do that as I am disconnected from the cave, please quit ***";

    @Before
    public void setup() {
        // Create the server tier
        Cave caveRemote = CommonCaveTests.createTestDoubledConfiguredCave();

        Invoker srh = new StandardInvoker(caveRemote);
        ClientRequestHandler properCrh = new LocalMethodCallClientRequestHandler(srh);
        saboteur = new SaboteurCRHDecorator(properCrh);

        cave = new CaveProxy(saboteur);
    }

    @Test
    public void shouldNotCrashWithException(){
        InputStream inputStream = IOUtils.toInputStream("n\ns\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", System.out, inputStream);

        saboteur.throwNextTime("Disconnected", new ConnectException());
        try {
            commandInterpreter.readEvalLoop();
            assertTrue(true);
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void shouldAskUserToDisconnectWhenServerUnresponsive(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);

        InputStream inputStream = IOUtils.toInputStream("n\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", printStream, inputStream);

        try {
            saboteur.throwNextTime("Disconnected", new ConnectException());
            commandInterpreter.readEvalLoop();
            String output = byteArrayOutputStream.toString("UTF-8");
            assertThat(output, containsString(disconnectedMessage));

            // Safety check
            inputStream.reset();
            byteArrayOutputStream.reset();
            output = byteArrayOutputStream.toString("UTF-8");
            assertThat(output, not(containsString(disconnectedMessage)));

            saboteur.throwNextTime("Disconnected", new NoRouteToHostException());
            commandInterpreter.readEvalLoop();
            output = byteArrayOutputStream.toString("UTF-8");
            assertThat(output, containsString(disconnectedMessage));

            // Safety check
            inputStream.reset();
            byteArrayOutputStream.reset();
            output = byteArrayOutputStream.toString("UTF-8");
            assertThat(output, not(containsString(disconnectedMessage)));

            saboteur.throwNextTime("Disconnected", new PortUnreachableException());
            commandInterpreter.readEvalLoop();
            output = byteArrayOutputStream.toString("UTF-8");
            assertThat(output, containsString(disconnectedMessage));
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void shouldKeepAskingTheUserToDisconnectWhenUnresponsive(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);

        InputStream inputStream = IOUtils.toInputStream("n\ns\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", printStream, inputStream);

        try {
            saboteur.throwNextTime("Disconnected", new ConnectException());
            commandInterpreter.readEvalLoop();
            String output = byteArrayOutputStream.toString("UTF-8");
            List<String> lines = Arrays.asList(output.split("\\n"));
            List<String> filteredLines = Lists.newArrayList(
                    Iterables.filter(lines, Predicates.equalTo(disconnectedMessage)));

            assertThat(filteredLines, hasItem(disconnectedMessage));
            assertThat(filteredLines, hasSize(3));
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }

    @Test
    public void shouldKeepAskingTheUserToDisconnectWhenUnresponsiveAndKeepsGoing(){
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
        PrintStream printStream = new PrintStream(byteArrayOutputStream);

        InputStream inputStream = IOUtils.toInputStream("n\ns\nn\ns\nn\ns\nn\n" +
                "s\nn\ns\nn\ns\nn\ns\nq\n");
        CmdInterpreter commandInterpreter = new CmdInterpreter(cave, "mikkel_aarskort", "123", printStream, inputStream);

        try {
            saboteur.throwNextTime("Disconnected", new ConnectException());
            commandInterpreter.readEvalLoop();
            String output = byteArrayOutputStream.toString("UTF-8");
            List<String> lines = Arrays.asList(output.split("\\n"));
            List<String> filteredLines = Lists.newArrayList(
                    Iterables.filter(lines, Predicates.equalTo(disconnectedMessage)));

            assertThat(filteredLines, hasItem(disconnectedMessage));
            assertThat(filteredLines, hasSize(15));
        }catch (Exception e){
            assertTrue(e.getMessage(), false);
        }
    }
}
