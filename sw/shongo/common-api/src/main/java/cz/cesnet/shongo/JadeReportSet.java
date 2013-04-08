package cz.cesnet.shongo;

import cz.cesnet.shongo.report.*;

/**
 * Auto-generated implementation of {@link AbstractReportSet}.
 *
 * @author cz.cesnet.shongo.tool-report-generator
 */
public class JadeReportSet extends AbstractReportSet
{
    /**
     * Unknown error: {@link #description}
     */
    public static class UnknownErrorReport implements Report
    {
        private String description;

        public String getDescription()
        {
            return description;
        }

        public void setDescription(String description)
        {
            this.description = description;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Unknown error: ${description}";
            message = message.replace("${description}", (description == null ? "" : description));
            return message;
        }
    }

    /**
     * Exception for {@link UnknownErrorReport}.
     */
    public static class UnknownErrorException extends AbstractReportException
    {
        private UnknownErrorReport report;

        public UnknownErrorException(UnknownErrorReport report)
        {
            this.report = report;
        }

        public UnknownErrorException(Throwable throwable, UnknownErrorReport report)
        {
            super(throwable);
            this.report = report;
        }

        public UnknownErrorException(String description)
        {
            report = new UnknownErrorReport();
            report.setDescription(description);
        }

        public UnknownErrorException(Throwable throwable, String description)
        {
            super(throwable);
            report = new UnknownErrorReport();
            report.setDescription(description);
        }

        public String getDescription()
        {
            return report.getDescription();
        }

        @Override
        public UnknownErrorReport getReport()
        {
            return report;
        }
    }
    /**
     * Receiver agent {@link #receiver-agent} is not available now.
     */
    public static class AgentNotFoundReport implements Report
    {
        private String receiverAgent;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} is not available now.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            return message;
        }
    }

    /**
     * Exception for {@link AgentNotFoundReport}.
     */
    public static class AgentNotFoundException extends AbstractReportException
    {
        private AgentNotFoundReport report;

        public AgentNotFoundException(AgentNotFoundReport report)
        {
            this.report = report;
        }

        public AgentNotFoundException(Throwable throwable, AgentNotFoundReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AgentNotFoundException(String receiverAgent)
        {
            report = new AgentNotFoundReport();
            report.setReceiverAgent(receiverAgent);
        }

        public AgentNotFoundException(Throwable throwable, String receiverAgent)
        {
            super(throwable);
            report = new AgentNotFoundReport();
            report.setReceiverAgent(receiverAgent);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        @Override
        public AgentNotFoundReport getReport()
        {
            return report;
        }
    }
    /**
     * Server agent {@link #sender-agent} is not started yet.
     */
    public static class AgentNotStartedReport implements Report
    {
        private String senderAgent;

        public String getSenderAgent()
        {
            return senderAgent;
        }

        public void setSenderAgent(String senderAgent)
        {
            this.senderAgent = senderAgent;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Server agent ${sender-agent} is not started yet.";
            message = message.replace("${sender-agent}", (senderAgent == null ? "" : senderAgent));
            return message;
        }
    }

    /**
     * Exception for {@link AgentNotStartedReport}.
     */
    public static class AgentNotStartedException extends AbstractReportException
    {
        private AgentNotStartedReport report;

        public AgentNotStartedException(AgentNotStartedReport report)
        {
            this.report = report;
        }

        public AgentNotStartedException(Throwable throwable, AgentNotStartedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public AgentNotStartedException(String senderAgent)
        {
            report = new AgentNotStartedReport();
            report.setSenderAgent(senderAgent);
        }

        public AgentNotStartedException(Throwable throwable, String senderAgent)
        {
            super(throwable);
            report = new AgentNotStartedReport();
            report.setSenderAgent(senderAgent);
        }

        public String getSenderAgent()
        {
            return report.getSenderAgent();
        }

        @Override
        public AgentNotStartedReport getReport()
        {
            return report;
        }
    }
    /**
     * Command {@link #command} has timeout.
     */
    public static class CommandTimeoutReport implements Report
    {
        private String receiverAgent;
        private String command;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Command ${command} has timeout.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            return message;
        }
    }

    /**
     * Exception for {@link CommandTimeoutReport}.
     */
    public static class CommandTimeoutException extends AbstractReportException
    {
        private CommandTimeoutReport report;

        public CommandTimeoutException(CommandTimeoutReport report)
        {
            this.report = report;
        }

        public CommandTimeoutException(Throwable throwable, CommandTimeoutReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandTimeoutException(String receiverAgent, String command)
        {
            report = new CommandTimeoutReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public CommandTimeoutException(Throwable throwable, String receiverAgent, String command)
        {
            super(throwable);
            report = new CommandTimeoutReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        @Override
        public CommandTimeoutReport getReport()
        {
            return report;
        }
    }
    /**
     * Receiver agent {@link #receiver-agent} doesn't implement command ${command}.
     */
    public static class CommandNotSupportedReport implements Report
    {
        private String receiverAgent;
        private String command;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} doesn't implement command ${command}.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            return message;
        }
    }

    /**
     * Exception for {@link CommandNotSupportedReport}.
     */
    public static class CommandNotSupportedException extends AbstractReportException
    {
        private CommandNotSupportedReport report;

        public CommandNotSupportedException(CommandNotSupportedReport report)
        {
            this.report = report;
        }

        public CommandNotSupportedException(Throwable throwable, CommandNotSupportedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandNotSupportedException(String receiverAgent, String command)
        {
            report = new CommandNotSupportedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public CommandNotSupportedException(Throwable throwable, String receiverAgent, String command)
        {
            super(throwable);
            report = new CommandNotSupportedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        @Override
        public CommandNotSupportedReport getReport()
        {
            return report;
        }
    }
    /**
     * Receiver agent {@link #receiver-agent} has refused command ${command}.
     */
    public static class CommandRefusedReport implements Report
    {
        private String receiverAgent;
        private String command;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} has refused command ${command}.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            return message;
        }
    }

    /**
     * Exception for {@link CommandRefusedReport}.
     */
    public static class CommandRefusedException extends AbstractReportException
    {
        private CommandRefusedReport report;

        public CommandRefusedException(CommandRefusedReport report)
        {
            this.report = report;
        }

        public CommandRefusedException(Throwable throwable, CommandRefusedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandRefusedException(String receiverAgent, String command)
        {
            report = new CommandRefusedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public CommandRefusedException(Throwable throwable, String receiverAgent, String command)
        {
            super(throwable);
            report = new CommandRefusedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        @Override
        public CommandRefusedReport getReport()
        {
            return report;
        }
    }
    /**
     * Receiver agent {@link #receiver-agent} didn't understand command ${command}.
     */
    public static class CommandNotUnderstoodReport implements Report
    {
        private String receiverAgent;
        private String command;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} didn't understand command ${command}.";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            return message;
        }
    }

    /**
     * Exception for {@link CommandNotUnderstoodReport}.
     */
    public static class CommandNotUnderstoodException extends AbstractReportException
    {
        private CommandNotUnderstoodReport report;

        public CommandNotUnderstoodException(CommandNotUnderstoodReport report)
        {
            this.report = report;
        }

        public CommandNotUnderstoodException(Throwable throwable, CommandNotUnderstoodReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandNotUnderstoodException(String receiverAgent, String command)
        {
            report = new CommandNotUnderstoodReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public CommandNotUnderstoodException(Throwable throwable, String receiverAgent, String command)
        {
            super(throwable);
            report = new CommandNotUnderstoodReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        @Override
        public CommandNotUnderstoodReport getReport()
        {
            return report;
        }
    }
    /**
     * Receiver agent {@link #receiver-agent} throws CommandException while processing command ${command}: ${reason}
     */
    public static class CommandFailedReport implements Report
    {
        private String receiverAgent;
        private String command;
        private String reason;

        public String getReceiverAgent()
        {
            return receiverAgent;
        }

        public void setReceiverAgent(String receiverAgent)
        {
            this.receiverAgent = receiverAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        public String getReason()
        {
            return reason;
        }

        public void setReason(String reason)
        {
            this.reason = reason;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Receiver agent ${receiver-agent} throws CommandException while processing command ${command}: ${reason}";
            message = message.replace("${receiver-agent}", (receiverAgent == null ? "" : receiverAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            message = message.replace("${reason}", (reason == null ? "" : reason));
            return message;
        }
    }

    /**
     * Exception for {@link CommandFailedReport}.
     */
    public static class CommandFailedException extends AbstractReportException
    {
        private CommandFailedReport report;

        public CommandFailedException(CommandFailedReport report)
        {
            this.report = report;
        }

        public CommandFailedException(Throwable throwable, CommandFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandFailedException(String receiverAgent, String command, String reason)
        {
            report = new CommandFailedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
            report.setReason(reason);
        }

        public CommandFailedException(Throwable throwable, String receiverAgent, String command, String reason)
        {
            super(throwable);
            report = new CommandFailedReport();
            report.setReceiverAgent(receiverAgent);
            report.setCommand(command);
            report.setReason(reason);
        }

        public String getReceiverAgent()
        {
            return report.getReceiverAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        public String getReason()
        {
            return report.getReason();
        }

        @Override
        public CommandFailedReport getReport()
        {
            return report;
        }
    }
    /**
     * Sender agent {@link #sender-agent} cannot decode response from command ${command}.
     */
    public static class CommandResultDecodingFailedReport implements Report
    {
        private String senderAgent;
        private String command;

        public String getSenderAgent()
        {
            return senderAgent;
        }

        public void setSenderAgent(String senderAgent)
        {
            this.senderAgent = senderAgent;
        }

        public String getCommand()
        {
            return command;
        }

        public void setCommand(String command)
        {
            this.command = command;
        }

        @Override
        public Type getType()
        {
            return Report.Type.ERROR;
        }

        @Override
        public String getMessage()
        {
            String message = "Sender agent ${sender-agent} cannot decode response from command ${command}.";
            message = message.replace("${sender-agent}", (senderAgent == null ? "" : senderAgent));
            message = message.replace("${command}", (command == null ? "" : command));
            return message;
        }
    }

    /**
     * Exception for {@link CommandResultDecodingFailedReport}.
     */
    public static class CommandResultDecodingFailedException extends AbstractReportException
    {
        private CommandResultDecodingFailedReport report;

        public CommandResultDecodingFailedException(CommandResultDecodingFailedReport report)
        {
            this.report = report;
        }

        public CommandResultDecodingFailedException(Throwable throwable, CommandResultDecodingFailedReport report)
        {
            super(throwable);
            this.report = report;
        }

        public CommandResultDecodingFailedException(String senderAgent, String command)
        {
            report = new CommandResultDecodingFailedReport();
            report.setSenderAgent(senderAgent);
            report.setCommand(command);
        }

        public CommandResultDecodingFailedException(Throwable throwable, String senderAgent, String command)
        {
            super(throwable);
            report = new CommandResultDecodingFailedReport();
            report.setSenderAgent(senderAgent);
            report.setCommand(command);
        }

        public String getSenderAgent()
        {
            return report.getSenderAgent();
        }

        public String getCommand()
        {
            return report.getCommand();
        }

        @Override
        public CommandResultDecodingFailedReport getReport()
        {
            return report;
        }
    }
}
