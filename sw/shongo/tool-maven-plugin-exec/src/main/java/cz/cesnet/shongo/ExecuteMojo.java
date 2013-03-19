package cz.cesnet.shongo;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.Executor;
import org.apache.commons.exec.PumpStreamHandler;
import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;

import java.io.IOException;

@Mojo(name = "execute")
public class ExecuteMojo extends AbstractMojo
{
    @Parameter(required = true)
    private String command;

    public void execute() throws MojoExecutionException
    {
        Executor executor = new DefaultExecutor();
        executor.setStreamHandler(new PumpStreamHandler(System.out, System.err, System.in));
        try {
            executor.execute(CommandLine.parse(command));
        }
        catch (IOException exception) {
            throw new MojoExecutionException("Command execution failed", exception);
        }
    }
}
