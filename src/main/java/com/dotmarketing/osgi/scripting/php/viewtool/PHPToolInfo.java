package com.dotmarketing.osgi.scripting.php.viewtool;

import org.apache.velocity.tools.view.context.ViewContext;
import org.apache.velocity.tools.view.servlet.ServletToolInfo;

public class PHPToolInfo extends ServletToolInfo{

	@Override
	public String getKey(){
		return "php";
	}

	@Override
    public String getScope () {
        return ViewContext.REQUEST;
    }

    @Override
    public String getClassname () {
        return PHPTool.class.getName();
    }

    @Override
    public Object getInstance ( Object initData ) {

    	PHPTool viewTool = new PHPTool();

        viewTool.init( initData );

        setScope( ViewContext.APPLICATION );

        return viewTool;
    }

}
