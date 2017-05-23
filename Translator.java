/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package app;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.util.List;
import java.io.StringReader;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Map;

import edu.stanford.nlp.process.Tokenizer;
import edu.stanford.nlp.process.TokenizerFactory;
import edu.stanford.nlp.process.CoreLabelTokenFactory;
import edu.stanford.nlp.process.PTBTokenizer;
import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.ling.Label;
import edu.stanford.nlp.trees.*;
import edu.stanford.nlp.parser.lexparser.LexicalizedParser;
import edu.stanford.nlp.trees.tregex.TregexMatcher;
import edu.stanford.nlp.trees.tregex.TregexPattern;
import edu.stanford.nlp.trees.tregex.tsurgeon.Tsurgeon;
import edu.stanford.nlp.trees.tregex.tsurgeon.TsurgeonPattern;
import edu.stanford.nlp.util.ArrayUtils;
import java.io.InputStream;

import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.util.logging.Level;
import java.util.logging.Logger;

public class Translator {

    String targetText;
    String profileDirectory = "/jas-libs/profiles/";
    HashMap<String, String[]> rules;

    public Translator(String s) throws IOException {
        this.rules = new HashMap<>();

        checkLanguage(s);
        String sourceText = s;
        initialiseRules();
        Tree parsed = tokeniseSentence(sourceText);
        targetText = findMatch(parsed);
        
    }

    public String returnText() {
        return targetText;
    }

    private Tree tokeniseSentence(String s) {
        String parserModel = "edu/stanford/nlp/models/lexparser/englishPCFG.ser.gz";
        LexicalizedParser lp = LexicalizedParser.loadModel(parserModel);

        TokenizerFactory<CoreLabel> tokenizerFactory = PTBTokenizer.factory(new CoreLabelTokenFactory(), "");
        Tokenizer<CoreLabel> tok = tokenizerFactory.getTokenizer(new StringReader(s));
        List<CoreLabel> rawWords2 = tok.tokenize();
        Tree parse = lp.apply(rawWords2);
        return parse;
    }

    private void checkLanguage(String s) {
        //JSONParser parser = new JSONParser();
        boolean isEnglish = false;
        String language = "English";
        try {
            String api;
            api = "http://apilayer.net/api/detect?access_key=7751c63a4edc293cf33d8b74629a1b0f&query=" + URLEncoder.encode(s, "UTF-8").replace("+", "%20");
            
            URL url = new URL(api);
            HttpURLConnection request = (HttpURLConnection) url.openConnection();
            JsonParser jp = new JsonParser(); //from gson
            JsonElement root = jp.parse(new InputStreamReader((InputStream) request.getContent())); //Convert the input stream to a json element
            JsonObject rootobj = root.getAsJsonObject(); //May be an array, may be an object. 
            String success = rootobj.get("success").getAsString();
            JsonArray results =  rootobj.getAsJsonArray("results");
            //JsonArray arr = results.getAsJsonArray();
            JsonObject propertiesJson = (JsonObject) results.get(0);
            String lang = propertiesJson.get("language_name").getAsString();
            
            if("English".equals(lang)){
                System.out.println("Language is English");
            }
            else{
                System.out.println("Language is not English");
                
            }
            
    }   catch (MalformedURLException ex) {
            Logger.getLogger(Translator.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IOException ex) {
            Logger.getLogger(Translator.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    private void initialiseRules() {
        String[] per1 = {"move sq $+ wh"};
        String[] per2 = {"delete vb"};
        String[] per3 = {"delete vb", "delete np"};
        rules.put("SBARQ<(/^WH/=wh $++ /^SQ/=sq)", per1);//What is your name? Who is yo
        rules.put("SQ<(/^VB/=vb $++ NP $++ ADJP)", per2);
        rules.put("SQ<(/^VB/=VB $++ NP $++ VP<(VB $++ /^NP/=np))", per3);
        System.out.println("Rules initialised");
    }

    private String findMatch(Tree parseTree) throws IOException {
        ArrayList<Label> sentence = new ArrayList<>();
        String resultantText = "";
        boolean translated = false;
        String rule;
        int i = 0;
        String[] array = null;

        for (Map.Entry<String, String[]> entry : rules.entrySet()) {
            rule = entry.getKey();
            TregexPattern p = TregexPattern.compile(rule);
            TregexMatcher m = p.matcher(parseTree);
            if (m.find()) {
                m.getMatch().pennPrint();
                List<TsurgeonPattern> ps = new ArrayList<>();
                String[] lst = entry.getValue();
                for (String s : lst) {
                    TsurgeonPattern op2 = Tsurgeon.parseOperation(s);
                    ps.add(op2);
                }
                Tree result = Tsurgeon.processPattern(p, Tsurgeon.collectOperations(ps), parseTree);
                result.pennPrint();
                sentence = result.yield();

                translated = true;
                break;
            }

        }
        if (!translated) {
            System.out.println("No Translation");
            System.out.println("Performing Direct Transfer");

            sentence = parseTree.yield();
        }
        //int count = 0;
        String[] chars = {",", ".", ";", ":", "%", "!", "@", "$", "?", "the", "is", "a", "do", "Do"};
        String sigml = "<sigml> \n ";

        for (Label l : sentence) {
            //resultantText += l + " ";
            if (ArrayUtils.contains(chars, l.value())) {
                continue;
            } else {
                resultantText += l + "\n";
                sigml += getSiGML(l);
                System.out.println(resultantText);
            }

        }

        sigml = sigml + "\n</sigml>";
        System.out.println(sigml);
        FileStreamer.stream(sigml, "localhost", 8052);

        return resultantText;

    }

    private String getSiGML(Label l) throws IOException {

        String name = "sigml/" + l + ".sigml"; //return name;
        String sigml = "";
        char letter;
        //File file = new File(name);

        //BufferedReader in = null;
        try {
            //in = new BufferedReader(new InputStreamReader(new FileInputStream(file)));
            sigml = readFile(name);
        } catch (FileNotFoundException f) {
            System.out.println("Word not found. Switching to fingerspeling");
            String fingerSpell = l.value();
            for (int i = 0; i < fingerSpell.length(); i++) {
                letter = fingerSpell.charAt(i);
                name = "sigml/" + letter + ".sigml";
                //in = new BufferedReader(new InputStreamReader(new FileInputStream("sigml/"+letter+".sigml")));
                sigml += readFile(name);
            }
        }

        return sigml;

    }

    public String readFile(String name) throws IOException {
        BufferedReader in = null;
        in = new BufferedReader(new InputStreamReader(new FileInputStream(name)));
        String sigml = "";
        String next_line = new String();

        if (in != null) {
            //send the data to the BAF player
            while (in.ready()) {
                next_line = in.readLine();
                sigml += next_line;
            }

        }
        return sigml;
    }

}
