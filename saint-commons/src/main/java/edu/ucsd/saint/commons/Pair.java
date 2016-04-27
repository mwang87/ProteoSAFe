package edu.ucsd.saint.commons;

public class Pair<T, S>{
	private T first;
	private S second;
	public Pair(T f, S s){ first = f; second = s; }
	public T getFirst(){ return first; }
	public S getSecond(){ return second; }
}
