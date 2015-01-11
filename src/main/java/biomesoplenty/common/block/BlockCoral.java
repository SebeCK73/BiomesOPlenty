/*******************************************************************************
 * Copyright 2014, the Biomes O' Plenty Team
 * 
 * This work is licensed under a Creative Commons Attribution-NonCommercial-NoDerivatives 4.0 International Public License.
 * 
 * To view a copy of this license, visit http://creativecommons.org/licenses/by-nc-nd/4.0/.
 ******************************************************************************/

package biomesoplenty.common.block;

import static net.minecraft.block.BlockLiquid.LEVEL;

import net.minecraft.block.material.Material;
import net.minecraft.block.properties.IProperty;
import net.minecraft.block.properties.PropertyEnum;
import net.minecraft.block.properties.PropertyInteger;
import net.minecraft.block.state.BlockState;
import net.minecraft.block.state.IBlockState;
import net.minecraft.util.IStringSerializable;
import biomesoplenty.api.block.BOPPlant;

public class BlockCoral extends BOPPlant
{
	public static PropertyEnum VARIANT_PROP = PropertyEnum.create("variant", CoralType.class);
	
	public BlockCoral()
	{
		super(Material.water);
		
    	this.setDefaultState(this.blockState.getBaseState().withProperty(VARIANT_PROP, CoralType.PINK));
	}

	@Override
	public IBlockState getStateFromMeta(int meta)
	{
		return this.getDefaultState().withProperty(VARIANT_PROP, CoralType.values()[meta]).withProperty(LEVEL, 15);
	}

	@Override
	public int getMetaFromState(IBlockState state)
	{
		int meta = ((CoralType)state.getValue(VARIANT_PROP)).ordinal();
		
		return meta;
	}

	@Override
	protected BlockState createBlockState()
	{
		return new BlockState(this, new IProperty[] { VARIANT_PROP, LEVEL });
	}
	
    @Override
    public IProperty[] getPresetProperties()
    {
    	return new IProperty[] { VARIANT_PROP };
    }
    
    @Override
	public String getStateName(IBlockState state, boolean fullName)
	{
    	CoralType type = (CoralType)state.getValue(VARIANT_PROP);
    	
		return type.getName() + (fullName && type != CoralType.ALGAE ? "_coral" : "");
	}
	
	public static enum CoralType implements IStringSerializable
	{
		PINK,
		ORANGE,
		BLUE,
		GLOWING,
		ALGAE;
		
        @Override
        public String getName()
        {
	        return this.name().toLowerCase();
        }

		@Override
		public String toString()
		{
			return getName();
		}
	}
}
