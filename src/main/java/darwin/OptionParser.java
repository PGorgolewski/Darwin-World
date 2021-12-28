package darwin;

import javafx.scene.control.TextField;
import java.util.HashMap;
import java.util.Map;

public class OptionParser {
    public static Map<String, Number> parseArguments(Map <String, TextField> menuTextFields) throws Exception {
        Map<String, Number> menuParsedMap = new HashMap<>();

        for (String optionName: menuTextFields.keySet()){
            if (optionName.equals("Use magic born [yes/no]"))
                menuParsedMap.put(optionName, parseMagicBorn(menuTextFields.get(optionName).getText()));
            else if (optionName.equals("Jungle ratio"))
                menuParsedMap.put(optionName, parseJungleRatio(menuTextFields.get(optionName).getText()));
            else
                menuParsedMap.put(optionName, parseIntegerValues(menuTextFields.get(optionName).getText()));
        }

        return menuParsedMap;
    }

    public static Number parseMagicBorn(String textFieldText) throws Exception {
        return switch (textFieldText.toLowerCase()){
            case "y", "yes" -> 1;
            case "n", "no" -> 0;
            default -> throw new Exception("Wrong magic born input. Possible ones: 'yes', 'y', 'no', 'n'");
        };
    }

    public static Number parseJungleRatio(String textFieldText) throws Exception {
        float ratio = Float.parseFloat(textFieldText);
        if (ratio < 0 || ratio > 1){
            throw new Exception("Jungle ratio must be between 0 and 1");
        }

        return ratio;
    }

    public static Number parseIntegerValues(String textFieldText) throws Exception {
        int inputParsedToInt = Integer.parseInt(textFieldText);

        if (inputParsedToInt < 0){
            throw new Exception(textFieldText + " can't be less than 0");
        }
        return inputParsedToInt;
    }
}
