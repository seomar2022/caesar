package com.cipher.caesar;

import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.*;

@Controller
public class CaesarCipherController {
    ////member variables
    //通常のアルファベット
    Character [] plainAlphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    //暗号化アルファベット
    Character [] cipherAlphabet = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j', 'k', 'l',
            'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x', 'y', 'z'};

    //constructor
    public CaesarCipherController(){}

    @GetMapping("/encode")
    public String returnEncodePage(){
        return "/encode";
    }

    @GetMapping("/decode")
    public String returnDecodePage(){
        return "/decode";
    }


    public String encode(String plainText, int shift){
        plainText = plainText.toLowerCase();
        return apply(plainText, switchAlphabetPosition(shift));
    }


    //overloading
    //int nが指定されなかった場合は１以上、２６以下の定数の中で、ランダムに適用する
    @PostMapping("/encode")
    public String encode(String plainText, RedirectAttributes redirectAttributes, int shift){

        System.out.println(shift);
        System.out.println(plainText);
        Random random = new Random();
        plainText = plainText.toLowerCase();

        String encodedResult;

        if (shift == 0){ //shiftが指定されてない場合はランダムに
            encodedResult = apply(plainText, switchAlphabetPosition(random.nextInt(plainAlphabet.length) + 1));
        }else{//shiftが指定された場合
            encodedResult = apply(plainText, switchAlphabetPosition(shift));
        }
        redirectAttributes.addFlashAttribute("encodedResult", encodedResult);
        return "redirect:/encode";
    }

    //正解の可能性が高い順にreturn
    @PostMapping("/decode")
    public String decode(String cipherText, RedirectAttributes redirectAttributes){
        HashMap<String, Integer> potentialAnswerWithScore = new HashMap<>(); //点数と単語を一緒に入れとく

        for(int i=0; i<plainAlphabet.length; i++){
            String potentialAnswer = encode(cipherText, i);
            //System.out.println(potentialAnswer);
            potentialAnswerWithScore.put(potentialAnswer, searchForWord(potentialAnswer));
            //System.out.println("====================");
        }

        ////searchForWord(potentialAnswer)のreturnが高い順で整列する
        List<String> keySetList = new ArrayList<>(potentialAnswerWithScore.keySet());//potentialAnswerWithScoreのkeyのみでArrayListを作くる

        //keySetListの要素の大きさを比較し、大きい順でkeySetListに格納する
        Collections.sort(keySetList, (o1, o2) -> (potentialAnswerWithScore.get(o2).compareTo(potentialAnswerWithScore.get(o1))));

        String[] sortedPotentialAnswers = new String[plainAlphabet.length];

        int i = 0;
        for(String key : keySetList) {
            //System.out.println("key: " + key + "| value: " + potentialAnswerWithScore.get(key));
            sortedPotentialAnswers[i] = key;
            i++;
        }
        /*Collections.sort(keySetList, (o1, o2) -> (map.get(o2).compareTo(map.get(o1))));
          keySetListを(o1, o2) -> (map.get(o2).compareTo(map.get(o1)))を基準に並び替える。
          */

        System.out.println(Arrays.toString(sortedPotentialAnswers));
        redirectAttributes.addFlashAttribute("sortedPotentialAnswers", sortedPotentialAnswers);

        //return sortedPotentialAnswers;
        return "redirect:/decode";
    }

    //平文の各文字を辞書順でn文字分ずらして暗号文とするmethod
    public Character [] switchAlphabetPosition(int shift){
        for(int i = 0; i<plainAlphabet.length; i++){
            cipherAlphabet[i] = plainAlphabet[(i+shift) % plainAlphabet.length];
        }
        return  cipherAlphabet;
    }

    //n文字分ずらしたアルファベットを適応するmethod
    public String apply(String plainText, Character [] cipherAlphabet){

        //indexOfを使うため、CharacterListをArrayListに変換
        ArrayList<Character> plainAlphabetList = new ArrayList<>(Arrays.asList(plainAlphabet));

        //暗号化された文字を入れるcharArrayを作る。lengthはplainTextと同じ。
        char [] encryptionArray = new char[plainText.length()];
        int i = 0;

        //平文から一つずつ文字を出して、その文字がplainAlphabetListの何番目に位置しているかを検索。
        // そして、同じ位置のcipherAlphabetの文字をencryptionArrayに入れる。
        for(char letter: plainText.toCharArray()){  //for文を使うため、StringをCharArrayにする。
            int j = plainAlphabetList.indexOf(letter);
            if(j == -1) {
                encryptionArray[i] = ' ';
            }else {
                encryptionArray[i] = cipherAlphabet[j];
            }

            i++; //encryptionArrayの次の場所に移る。
        }
        return new String(encryptionArray); //charArrayをStringに変換して、return
    }

    //単語が辞書にあったらscoreに１をたす。
    public int searchForWord(String potentialAnswer){
        String[] splitPotentialAnswer = potentialAnswer.split(" ");
        int score = 0;
        String URL = "https://www.dictionary.com/browse/";
        for (int i = 0; i < splitPotentialAnswer.length; i++) {
            String searchedWord = splitPotentialAnswer[i];
            // System.out.println(searchedWord);
            try {
                Jsoup.connect(URL + searchedWord).get();
                score += 1;
                // System.out.println("↑○　辞書にある単語");
            } catch (HttpStatusException httpStatusException) {
                //IOExceptionの中にHttpStatusExceptionがあるので、IOExceptionより先にHttpStatusExceptionをcatch
                //  System.out.println("↑X　辞書にない単語");
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        //平文の確率が高い順で、arrayに入れる。同じ確率であれば、アルファベット順に。

        return score;
    }
}
