package cn.ligl.lectureaid.model;

/***
 * Transact information carrier</br> <b>carrier type :</b>XML & JSON
 * 
 * @author ligl
 * @email ligl95403@gmail.com
 */
public final class InformationCarrier {
	private static InformationCarrier sInformationCarrier;
	public static int TYPE_XML = 1;
	public static int TYPE_JSON = 2;
	private int mCarrierType = TYPE_JSON;

	public static InformationCarrier getInstance() {
		if (sInformationCarrier == null) {
			synchronized (sInformationCarrier) {
				sInformationCarrier = new InformationCarrier();
			}
		}
		return sInformationCarrier;
	}

	public void setCarrierType(int type) {
		mCarrierType = type;
	}

	public int getCarrierType() {
		return mCarrierType;
	}

	private InformationCarrier() {
	}
}
