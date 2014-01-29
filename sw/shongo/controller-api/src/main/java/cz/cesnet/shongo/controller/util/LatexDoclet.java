package cz.cesnet.shongo.controller.util;

//import com.sun.javadoc.Doclet;

/**
 * Class for generating API documentation to LaTeX.
 * <p/>
 * TODO: Not fully implemented
 *
 * @author Martin Srom <martin.srom@cesnet.cz>
 */
public class LatexDoclet /*extends Doclet*/
{
    /*private static final List<String> SUPERCLASSES = new ArrayList<String>()
    {{
            add("IdentifiedComplexType");
        }};

    private Map<String, ClassDoc> classDocByName = new HashMap<String, ClassDoc>();

    private List<String> classOrder = new ArrayList<String>();

    public String format()
    {
        StringBuilder stringBuilder = new StringBuilder();
        String separator = "\n";

        // Append all classes from order
        for (String className : classOrder) {
            ClassDoc classDoc = classDocByName.get(className);
            if (classDoc == null) {
                throw new RuntimeException(className);
            }
            classDocByName.remove(className);

            if (stringBuilder.length() > 0) {
                stringBuilder.append(separator);
            }
            stringBuilder.append(formatType(classDoc));
        }

        // Get al remaining classes
        List<String> classNames = new ArrayList<String>();
        classNames.addAll(classDocByName.keySet());
        Collections.sort(classNames);

        // Append all remaining classes and print warning
        StringBuilder warning = new StringBuilder();
        for (String className : classNames) {
            ClassDoc classDoc = classDocByName.get(className);
            if (stringBuilder.length() > 0) {
                stringBuilder.append(separator);
            }
            if (warning.length() > 0) {
                warning.append("\n");
            }
            warning.append(className);
            stringBuilder.append(formatType(classDoc));
        }
        System.out.println("Following classes weren't found in the order and was placed at the end:");
        System.out.println(warning.toString());

        return stringBuilder.toString();
    }

    private String formatType(ClassDoc classDoc)
    {
        if (getClassType(classDoc).isEnum()) {
            return formatEnum(classDoc);
        }
        else {
            return formatClass(classDoc);
        }
    }

    private String formatEnum(ClassDoc classDoc)
    {
        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append(String.format("\\ApiEnum{%s}\n", classDoc.name()));
        stringBuilder.append(String.format("%s\n", formatComment(classDoc.commentText())));
        stringBuilder.append("\\begin{ApiEnumValues}\n");
        for (FieldDoc fieldDoc : classDoc.fields()) {
            stringBuilder.append(String.format("\\ApiEnumValue{%s}{} %s\n", fieldDoc.name(),
                    formatComment(fieldDoc.commentText())));
        }
        stringBuilder.append("\\end{ApiEnumValues}\n");
        return stringBuilder.toString();
    }

    public String formatClass(ClassDoc classDoc)
    {
        StringBuilder stringBuilder = new StringBuilder();
        String superClassName = "";
        if (classDoc.superclass() != null) {
            superClassName = classDoc.superclass().name();
        }
        stringBuilder.append(String.format("\\ApiClass{%s}{%s}\n", classDoc.name(), superClassName));
        stringBuilder.append(String.format("%s\n", formatComment(classDoc.commentText())));

        stringBuilder.append("\\begin{ApiClassAttributes}\n");

        if (classDoc.superclass() != null && SUPERCLASSES.contains(classDoc.superclass().name())) {
            stringBuilder.append(formatClassAttributes(classDoc.superclass()));
        }
        stringBuilder.append(formatClassAttributes(classDoc));
        stringBuilder.append("\\end{ApiClassAttributes}\n");

        return stringBuilder.toString();
    }

    public String formatClassAttributes(ClassDoc classDoc)
    {
        StringBuilder stringBuilder = new StringBuilder();
        for (FieldDoc fieldDoc : classDoc.fields()) {
            String propertyName = fieldDoc.name();
            propertyName = getCamelCase(propertyName);
            String format = formatClassAttribute(propertyName, fieldDoc, classDoc);
            if (format != null) {
                stringBuilder.append(format);
            }
        }
        return stringBuilder.toString();
    }

    public String formatClassAttribute(String propertyName, FieldDoc fieldDoc, ClassDoc classDoc)
    {
        // Skip changeable properties
        if (propertyName.equals("propertyStorage") || propertyName.equals("changesTracking")) {
            return null;
        }
        Class classType = getClassType(classDoc);
        Property property = Property.getProperty(classType, propertyName);
        // Skip not properties
        if (property == null) {
            return null;
        }
        String propertyType = ClassHelper.getClassShortName(property.getType());
        String propertyDescription = formatComment(fieldDoc.commentText());

        boolean changeableAttribute =
                IdentifiedChangeableObject.class.isAssignableFrom(classType) && property.getField() == null;

        String propertyOption = "\\ApiOptional";
        if (property.getAnnotation(Required.class) != null || propertyName.equals("id")) {
            propertyOption = "\\ApiRequired";
        }
        else if (property.getAnnotation(ReadOnly.class) != null || !changeableAttribute) {
            propertyOption = "\\ApiReadOnly";
        }

        if (TypeFlags.isArrayOrCollection(property.getTypeFlags())) {
            StringBuilder valueType = new StringBuilder();
            for (Class allowedType : property.getValueAllowedTypes()) {
                if (valueType.length() > 0) {
                    valueType.append("|");
                }
                valueType.append(ClassHelper.getClassShortName(allowedType));
            }
            return String.format("\\ApiClassAttributeCollection{%s}{%s}{%s}{%s} %s\n", propertyName, propertyType,
                    valueType.toString(), propertyOption, propertyDescription);
        }
        else if (TypeFlags.isMap(property.getTypeFlags())) {
            String keyType = ClassHelper.getClassShortName(property.getKeyAllowedType());
            StringBuilder valueType = new StringBuilder();
            for (Class allowedType : property.getValueAllowedTypes()) {
                if (valueType.length() > 0) {
                    valueType.append("|");
                }
                valueType.append(ClassHelper.getClassShortName(allowedType));
            }
            return String.format("\\ApiClassAttributeCollection{%s}{%s}{%s}{%s}{%s} %s\n", propertyName, propertyType,
                    keyType, valueType.toString(), propertyOption, propertyDescription);
        }
        else {
            return String.format("\\ApiClassAttribute{%s}{%s}{%s} %s\n", propertyName, propertyType, propertyOption,
                    propertyDescription);
        }
    }

    private static final Pattern PATTERN_LINK = Pattern.compile("\\{@link (.+)\\}");

    public String formatComment(String comment)
    {
        Matcher matcher = PATTERN_LINK.matcher(comment);
        while (matcher.find()) {
            MatchResult matchResult = matcher.toMatchResult();
            String value = matchResult.group(1);
            value = String.format("\\ApiRef{%s}", value);

            StringBuilder builder = new StringBuilder();
            builder.append(comment.substring(0, matchResult.start()));
            builder.append(value);
            builder.append(comment.substring(matchResult.end()));

            comment = builder.toString();
            matcher.reset(comment);
        }
        return comment;
    }

    public Class getClassType(ClassDoc classDoc)
    {
        try {
            String typeName = classDoc.containingPackage().name() + "." + classDoc.name().replace(".", "$");
            return Class.forName(typeName);
        }
        catch (ClassNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    public String getCamelCase(String value)
    {
        String[] parts = value.split("_|(?<!(^|[A-Z]))(?=[A-Z])|(?<!^)(?=[A-Z][a-z])");
        StringBuilder camelCase = new StringBuilder();
        for (String part : parts) {
            if (part.length() == 0) {
                continue;
            }
            String firstLetter = part.substring(0, 1);
            if (camelCase.length() == 0) {
                firstLetter = firstLetter.toLowerCase();
            }
            else {
                firstLetter = firstLetter.toUpperCase();
            }
            camelCase.append(firstLetter);
            camelCase.append(part.substring(1).toLowerCase());
        }
        return camelCase.toString();
    }

    public static boolean start(RootDoc root)
    {
        LatexDoclet texDoclet = new LatexDoclet();
        ClassDoc[] classes = root.classes();
        for (int i = 0; i < classes.length; ++i) {
            ClassDoc classDoc = classes[i];
            texDoclet.classDocByName.put(classDoc.name(), classDoc);
        }

        // Parse options
        String fileName = null;
        for (String[] option : root.options()) {
            if (option[0].equals("-filename")) {
                fileName = option[1];
            }
            else if (option[0].equals("-order")) {
                for (String classItem : option[1].split(":")) {
                    texDoclet.classOrder.add(classItem);
                }
            }
        }
        if (fileName == null) {
            throw new RuntimeException("Filename must be specified!");
        }

        // Format classes
        String content = texDoclet.format();

        // Write it to file
        FileWriter fileWriter = null;
        try {
            File newTextFile = new File(fileName);
            fileWriter = new FileWriter(newTextFile);
            fileWriter.write(content);
            fileWriter.close();
        }
        catch (IOException exception) {
            throw new RuntimeException(exception);
        }
        finally {
            try {
                if (fileWriter != null) {
                    fileWriter.close();
                }
            }
            catch (IOException exception) {
                throw new RuntimeException(exception);
            }
        }
        return true;
    }

    public static int optionLength(String option)
    {
        if (option.equals("-filename")) {
            return 2;
        }
        else if (option.equals("-order")) {
            return 2;
        }
        return Doclet.optionLength(option);
    }

    public static void execute(String fileName, String[] packageNames, String[] classOrder)
    {
        StringBuilder builder = new StringBuilder();
        for (String classItem : classOrder) {
            if (builder.length() > 0) {
                builder.append(":");
            }
            builder.append(classItem.trim());
        }

        List<String> arguments = new ArrayList<String>();
        arguments.add("-private");
        arguments.add("-sourcepath");
        arguments.add("controller-api/src/main/java:common-api/src/main/java");
        arguments.add("-filename");
        arguments.add(fileName);
        arguments.add("-order");
        arguments.add(builder.toString());
        arguments.add("cz.cesnet.shongo");
        arguments.add("cz.cesnet.shongo.api");
        arguments.add("cz.cesnet.shongo.api.util");
        for (String packageName : packageNames) {
            arguments.add(packageName);
        }
        com.sun.tools.javadoc.Main.execute("Shongo", "cz.cesnet.shongo.controller.util.TexDoclet",
                arguments.toArray(new String[arguments.size()]));
    }

    public static void main(String[] args) throws Exception
    {
        execute(
                "../../doc/shongo/api/controller_classes.tex",
                new String[]{
                        "cz.cesnet.shongo.controller",
                        "cz.cesnet.shongo.controller.api"
                },
                new String[]{
                        "Controller", "Domain", "Connector", "Status",

                        "DateTimeSlot", "Alias", "AliasType", "CallInitiation",

                        "Resource", "DeviceResource", "ManagedMode",
                        "Capability", "RoomProviderCapability", "StandaloneTerminalCapability",
                        "AliasProviderCapability",

                        "ReservationRequestPurpose", "ReservationRequestState",
                        "AbstractReservationRequest", "ReservationRequest", "ReservationRequestSet",

                        "Specification", "ResourceSpecification", "AliasSpecification", "RoomSpecification",
                        "CompartmentSpecification", "AbstractParticipant", "ExternalEndpointParticipant",
                        "ExternalEndpointSetParticipant", "ExistingEndpointParticipant",
                        "LookupEndpointParticipant", "PersonParticipant", "InvitedPersonParticipant",

                        "ReservationRequestSummary.Type", "ReservationRequestSummary",

                        "Reservation", "ResourceReservation", "RoomReservation", "AliasReservation",
                        "ExistingReservation",

                        "Executable", "Executable.State", "CompartmentExecutable", "EndpointExecutable",
                        "RoomExecutable", "ConnectionExecutable",

                        "ResourceAllocation", "RoomProviderResourceAllocation"
                });

    }*/
}
