package org.wildfly.apigen.test.invocation.datasources.subsystem.xaDataSource.xaDatasourceProperties;

import org.wildfly.swarm.config.runtime.Address;
import org.wildfly.swarm.config.runtime.ModelNodeBinding;
/**
 * List of xa-datasource-property
 */
@Address("/subsystem=datasources/xa-data-source=*/xa-datasource-properties=*")
public class XaDatasourceProperties<T extends XaDatasourceProperties> {

	private String key;
	private String value;

	public XaDatasourceProperties(String key) {
		this.key = key;
	}

	public String getKey() {
		return this.key;
	}

	/**
	 * Specifies a property value to assign to the XADataSource implementation class. Each property is identified by the name attribute and the property value is given by the xa-datasource-property element content. The property is mapped onto the XADataSource implementation by looking for a JavaBeans style getter method for the property name. If found, the value of the property is set using the JavaBeans setter with the element text translated to the true property type using the java.beans.PropertyEditor
	 */
	@ModelNodeBinding(detypedName = "value")
	public String value() {
		return this.value;
	}

	/**
	 * Specifies a property value to assign to the XADataSource implementation class. Each property is identified by the name attribute and the property value is given by the xa-datasource-property element content. The property is mapped onto the XADataSource implementation by looking for a JavaBeans style getter method for the property name. If found, the value of the property is set using the JavaBeans setter with the element text translated to the true property type using the java.beans.PropertyEditor
	 */
	@SuppressWarnings("unchecked")
	public T value(String value) {
		this.value = value;
		return (T) this;
	}
}