#set( $symbol_pound = '#' )
#set( $symbol_dollar = '$' )
#set( $symbol_escape = '\' )
package ${package};

import eu.arrowhead.common.api.ArrowheadApplication;
import eu.arrowhead.common.exception.ArrowheadException;

public class Application extends ArrowheadApplication {

  /**
   * @param args Arguments from main().
   */
  public Application(String[] args) throws ArrowheadException {
    super(args);
  }

  @Override
  protected void onStart() throws ArrowheadException {

  }

  @Override
  protected void onStop() {

  }

  public static void main(String[] args) throws ArrowheadException {
    new Application(args).start();
  }
}
