package me.lordsaad.wizardry.shader;

public abstract class ShaderCallback<T extends Shader> {

    public abstract void call(T shader);

}
