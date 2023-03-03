package org.tantama.anchoco.springcrib.kintone;

import java.time.ZonedDateTime;

import lombok.Getter;
import lombok.Setter;

/**
 * Kintonneから取得したjsonのDto
 */
@Getter
@Setter
public class KintonePojo {

	/** レコードID */
	private int recordId;
	/** 会社名 */
	private String companyName;

	/** 郵便番号 */
	private String zipCode;

	private ZonedDateTime updateDatetime;
}
