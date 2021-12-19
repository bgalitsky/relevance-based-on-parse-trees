
package opennlp.tools.chatbot.text_searcher;

import java.util.Comparator;

import opennlp.tools.chatbot.ChatIterationResult;

public class ChatIterationResultComparable implements Comparator<ChatIterationResult> {
  // @Override
  public int compare(ChatIterationResult o1, ChatIterationResult o2) {
    return (o1.getGenerWithQueryScore() > o2.getGenerWithQueryScore() ? -1
        : (o1 == o2 ? 0 : 1));
  }
}