public class TypedValue {
  public final String TYPE; // TODO: Turn this into an enum?
  public final String VALUE;

  public TypedValue(String type, String value) {
    this.TYPE = type;
    this.VALUE = value;
  }
}
