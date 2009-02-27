/*******************************************************************************
 * Copyright (c) 2008, 2009 IBM Corporation and others.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License v1.0
 * which accompanies this distribution, and is available at
 * http://www.eclipse.org/legal/epl-v10.html
 *
 * Contributors:
 *     IBM Corporation - initial API and implementation
 *******************************************************************************/
package a.b.c;

/**
 * Test unsupported @noreference tag on methods in outer / inner annotations
 */
public @interface test10 {
	@interface inner {
		/**
		 * @noreference
		 * @return
		 */
		public int m1();
		
		/**
		 * @noreference
		 * @return
		 */
		public abstract char m2();
		@interface inner2 {
			/**
			 * @noreference
			 * @return
			 */
			public int m1();
			
			/**
			 * @noreference
			 * @return
			 */
			public abstract char m2();
		}
	}
}

@interface outer {
	/**
	 * @noreference
	 * @return
	 */
	public int m1();
	
	/**
	 * @noreference
	 * @return
	 */
	public abstract char m2();
}
